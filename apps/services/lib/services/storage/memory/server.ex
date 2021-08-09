defmodule Services.Storage.Memory.Server do
  use GenServer

  alias Services.State
  def start_link(_), do: GenServer.start_link(__MODULE__, nil, name: __MODULE__)

  @impl true
  def init(_), do: {:ok, State.new()}

  defp aggregator, do: Application.fetch_env!(:services, :aggregator)

  def available_services, do: GenServer.call(__MODULE__, :available_services)

  def service(id), do: GenServer.call(__MODULE__, {:service, id})

  def dispatch_events(events), do: GenServer.call(__MODULE__, {:dispatch_events, events})

  @impl true
  def handle_call(:available_services, _from, state),
    do: {:reply, {:ok, State.available_services(state)}, state}

  @impl true
  def handle_call({:service, id}, _from, state) do
    response =
      case State.service(state, id) do
        nil -> {:error, :not_found}
        service -> {:ok, service}
      end

    {:reply, response, state}
  end

  @impl true
  def handle_call({:dispatch_events, events}, _from, state) do
    state = Enum.reduce(events, state, &dispatch_event/2)
    {:reply, :ok, state}
  end

  defp dispatch_event(event, state) do
    {state, events} = State.reduce(state, event)
    aggregator().append_events(events)
    state
  end
end
