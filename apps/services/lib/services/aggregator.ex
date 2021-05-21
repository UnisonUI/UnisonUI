defmodule Services.Aggregator do
  use GenStage
  require Logger
  alias Common.Service
  alias Common.Events
  alias Database.Schema.State

  @task Services.TaskSupervisor
  @events_to_reduce 100
  @interval 1_000
  defstruct last_id: 0, services: %{}, queue: :queue.new(), pending: 0

  def start_link, do: GenStage.start_link(__MODULE__, 0, name: __MODULE__)

  @spec available_services :: [Common.Service.t()]
  def available_services, do: GenStage.call(__MODULE__, :available_services)

  @spec service(id :: String.t()) :: {:ok, Common.Service.t()} | {:error, :not_found}
  def service(id), do: GenStage.call(__MODULE__, {:service, id})

  defp schedule, do: Process.send_after(self(), :schedule, @interval)

  @impl true
  def init(_) do
    latest_state =
      Database.transaction(fn ->
        case Database.last(State) do
          :"$end_of_table" -> %__MODULE__{}
          key -> %__MODULE__{last_id: key, services: Database.read(State, key)}
        end
      end)

    state =
      case latest_state do
        {:ok, state} -> state
        _ -> %__MODULE__{}
      end

    _ = schedule()
    {:producer, state, dispatcher: GenStage.BroadcastDispatcher}
  end

  @impl true
  def handle_info(
        :schedule,
        %__MODULE__{last_id: current_id, queue: queue, services: services} = state
      ) do
    {queue, last_id, services} =
      Database.transaction(fn -> Database.Schema.Events.all_after(current_id) end)
      |> Enum.reduce({queue, current_id, services}, fn %Database.Schema.Events{
                                                         id: id,
                                                         event: event
                                                       },
                                                       {queue, current_id, services} ->
        last_id = max(current_id, id)

        {events, services} =
          case event do
            %Events.Up{service: %{id: id} = service} ->
              Logger.debug("New service: #{service.id}")
              service_up = event
              service_down = %Events.Down{id: id}

              events =
                case {named_changed?(services, service), new_service?(services, service),
                      content_changed?(services, service)} do
                  {_, _, true} -> [%Events.Changed{id: id}]
                  {true, _, _} -> [service_down, service_up]
                  {_, true, _} -> [service_up]
                  _ -> []
                end

              services = Map.update(services, id, service, fn _ -> service end)
              {events, services}

            %Events.Down{id: id} = event ->
              {Map.delete(services, id), [event]}
          end

        {Enum.reduce(events, queue, &:queue.in/2), last_id, services}
      end)

    {events, state} =
      dispatch_events(%__MODULE__{state | queue: queue, services: services, last_id: last_id}, [])

    _ = compact_state(current_id, last_id, services)
    _ = schedule()
    {:noreply, events, state}
  end

  defp dispatch_events(%__MODULE__{pending: 0} = state, events),
    do: {Enum.reverse(events), state}

  defp dispatch_events(%__MODULE__{queue: queue, pending: pending} = state, events) do
    case :queue.out(queue) do
      {{:value, event}, queue} ->
        dispatch_events(%__MODULE__{state | pending: pending - 1, queue: queue}, [event | events])

      {:empty, queue} ->
        {Enum.reverse(events), %__MODULE__{state | queue: queue}}
    end
  end

  @impl true
  def handle_call(:available_services, from, state) do
    _ =
      Task.Supervisor.start_child(@task, fn ->
        response = Enum.into([], fn {_, service} -> %Events.Up{service: service} end)

        GenServer.reply(from, response)
      end)

    {:noreply, [], state}
  end

  def handle_call({:service, id}, from, %__MODULE__{services: services} = state) do
    _ =
      Task.Supervisor.start_child(@task, fn ->
        response =
          case Map.get(services, id) do
            nil -> {:error, :not_found}
            service -> {:ok, service}
          end

        GenServer.reply(from, response)
      end)

    {:noreply, [], state}
  end

  @impl true
  def handle_demand(incoming_demand, %__MODULE__{pending: pending} = state) do
    {events, state} = dispatch_events(%__MODULE__{state | pending: pending + incoming_demand}, [])
    {:noreply, events, state}
  end

  @spec compact_state(current_id :: pos_integer(), last_id :: pos_integer(), state :: map()) ::
          term()
  defp compact_state(current_id, last_id, state) when last_id - current_id >= @events_to_reduce do
    _ =
      Task.Supervisor.start_child(@task, fn ->
        _ =
          Database.transaction(fn ->
            last = Database.last(State)

            if last_id > last do
              _ = Database.write(%State{id: last_id, state: state})

              last_id
              |> Database.Schema.Events.all_before()
              |> Enum.each(&Database.delete_record/1)
            end
          end)
      end)
  end

  defp compact_state(_current_id, _last_id, _state), do: nil

  @spec named_changed?(services :: %{String.t() => Service.t()}, Service.t()) :: boolean()
  defp named_changed?(services, %{id: id, name: name}),
    do:
      Enum.any?(
        services,
        &match?({^id, %{name: service_name}} when service_name != name, &1)
      )

  defp named_changed?(_, _), do: false

  @spec new_service?(services :: %{String.t() => Service.t()}, Service.t()) :: boolean()
  defp new_service?(services, %{id: id}), do: !Map.has_key?(services, id)

  @spec content_changed?(services :: %{String.t() => Service.t()}, Service.t()) :: boolean()
  defp content_changed?(services, service),
    do:
      Enum.any?(services, fn {id, current_service} ->
        id == service.id &&
          Service.compute_hash(current_service) != Service.compute_hash(service)
      end)
end
