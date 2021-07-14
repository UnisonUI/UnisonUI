defmodule Services.Storage.Memory.Server do
  use GenServer

  alias Common.{Events, Service}
  def start_link(_), do: GenServer.start_link(__MODULE__, nil, name: __MODULE__)

  @impl true
  def init(_), do: {:ok, %{}}
  defp aggregator, do: Application.fetch_env!(:services, :aggregator)
  def available_services, do: GenServer.call(__MODULE__, :available_services)
  def service(id), do: GenServer.call(__MODULE__, {:service, id})
  def dispatch_events(events), do: GenServer.call(__MODULE__, {:dispatch_events, events})

  @impl true
  def handle_call(:available_services, _from, state) do
    services = Enum.into(state, [], fn {_, service} -> %Events.Up{service: service} end)
    {:reply, {:ok, services}, state}
  end

  @impl true
  def handle_call({:service, id}, _from, state) do
    response =
      case state[id] do
        nil -> {:error, :not_found}
        service -> {:ok, service}
      end

    {:reply, response, state}
  end

  @impl true
  def handle_call({:dispatch_events, events}, _from, state) do
    state = Enum.reduce(events , state, &dispatch_event/2)
    {:reply, :ok, state}
  end

  defp dispatch_event(event, services) do
    {events, services} =
      case event do
        %Events.Up{service: %{id: id} = service} ->
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
          {[event], Map.delete(services, id)}
      end
    aggregator().append_events(events)
    services
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
