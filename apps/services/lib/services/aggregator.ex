defmodule Services.Aggregator do
  use GenStage
  require Logger

  @interval 1_000

  def start_link(_), do: GenStage.start_link(__MODULE__, :ok, name: __MODULE__)

  @callback append_events(events :: [term()]) :: :ok
  def append_events(events), do: GenStage.cast(__MODULE__, {:append_events, events})

  defp schedule, do: Process.send_after(self(), :schedule, @interval)

  @impl true
  def init(_) do
    _ = schedule()
    {:producer, {:queue.new(), 0}, dispatcher: GenStage.BroadcastDispatcher}
  end

  @impl true
  def handle_info(:schedule, {queue, pending}) do
    {events, state} = dispatch_events(queue, pending, [])
    _ = schedule()
    {:noreply, events, state}
  end

  defp dispatch_events(queue, 0, events), do: {Enum.reverse(events), {queue, 0}}

  defp dispatch_events(queue, pending, events) do
    case :queue.out(queue) do
      {{:value, event}, queue} ->
        dispatch_events(queue, pending - 1, [event | events])

      {:empty, queue} ->
        {Enum.reverse(events), {queue, pending}}
    end
  end

  @impl true
  def handle_cast({:append_events, events}, {queue, pending}) do
    {events, state} = events |> Enum.reduce(queue, &:queue.in/2) |> dispatch_events(pending, [])
    {:noreply, events, state}
  end

  @impl true
  def handle_demand(incoming_demand, {queue, pending}) do
    {events, state} = dispatch_events(queue, pending + incoming_demand, [])
    {:noreply, events, state}
  end
end
