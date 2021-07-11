defmodule ServiceStub do
  use GenServer
  @behaviour Services.Behaviour
  def start_link(pid), do: GenServer.start_link(__MODULE__, pid, name: __MODULE__)

  @impl true
  def init(pid), do: {:ok, pid}

  def available_services, do: {:error, :not_used}
  def service(_), do: {:error, :not_used}
  def dispatch_events(events), do: GenServer.call(__MODULE__, events)

  @impl true
  def handle_call(events, _from, pid) do
    Enum.each(events, &send(pid, &1))
    {:reply, :ok, pid}
  end
end
