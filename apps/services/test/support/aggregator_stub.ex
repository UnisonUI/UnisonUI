defmodule AggregatorStub do
  use GenServer
  @behaviour Services.Aggregator
  def start_link(pid), do: GenServer.start_link(__MODULE__, pid, name: __MODULE__)

  @impl true
  def init(pid), do: {:ok, pid}

  @impl true
  def append_events(e), do: GenServer.call(__MODULE__, e)

  @impl true
  def handle_call(events, _from, pid) do
    Enum.each(events, &send(pid, &1))
    {:reply, :ok, pid}
  end
end
