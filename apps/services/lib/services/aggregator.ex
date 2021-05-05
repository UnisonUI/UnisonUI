defmodule Services.Aggregator do
  use GenStage
  require Logger
  alias Common.Service
  alias Common.Events

  @task Services.TaskSupervisor

  def start_link, do: GenStage.start_link(__MODULE__, %{}, name: __MODULE__)

  @spec available_services :: [Common.Service.t()]
  def available_services, do: GenStage.call(__MODULE__, :available_services)

  @spec service(id :: String.t()) :: {:ok, Common.Service.t()} | {:error, :not_found}
  def service(id), do: GenStage.call(__MODULE__, {:service, id})

  @impl true
  def init(state), do: {:producer_consumer, state, dispatcher: GenStage.BroadcastDispatcher}

  @impl true
  def handle_call(:available_services, from, state) do
    _ =
      Task.Supervisor.start_child(@task, fn ->
        response =
          state
          |> Stream.map(fn {_, service} -> %Events.Up{service: service} end)
          |> Enum.to_list()

        GenServer.reply(from, response)
      end)

    {:noreply, [], state}
  end

  def handle_call({:service, id}, from, state) do
    _ =
      Task.Supervisor.start_child(@task, fn ->
        response =
          case Map.get(state, id) do
            nil -> {:error, :not_found}
            service -> {:ok, service}
          end

        GenServer.reply(from, response)
      end)

    {:noreply, [], state}
  end

  @impl true
  def handle_events(events, _from, state) do
    {new_state, events_to_dispatch} =
      events
      |> Enum.reduce({state, []}, fn
        %Events.Up{service: %{id: id} = service} = event, {services, events} ->
          Logger.debug("New service: #{service.id}")
          service_up = event
          service_down = %Events.Down{id: id}

          new_events =
            case {named_changed?(services, service), new_service?(services, service),
                  content_changed?(services, service)} do
              {_, _, true} -> [%Events.Changed{id: id}]
              {true, _, _} -> [service_down, service_up]
              {_, true, _} -> [service_up]
              _ -> []
            end

          new_state = Map.update(services, id, service, fn _ -> service end)
          {new_state, new_events ++ events}

        %Events.Down{id: id} = event, {map, events} ->
          {map |> Map.delete(id), [event | events]}
      end)

    {:noreply, events_to_dispatch, new_state}
  end

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
