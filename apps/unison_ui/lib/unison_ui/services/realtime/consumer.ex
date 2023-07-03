defmodule UnisonUI.Services.Realtime.Consumer do
  use GenStage

  def start_link(pid), do: GenStage.start_link(__MODULE__, pid)

  @impl true
  def init(pid) do
    Process.monitor(pid)
    {:consumer, pid, subscribe_to: [Application.fetch_env!(:services, :aggregator)]}
  end

  @impl true
  def handle_events(events, _from, pid) do
    events
    |> Stream.map(&Jason.encode!/1)
    |> Enum.each(fn event -> send(pid, {:event, event}) end)

    {:noreply, [], pid}
  end

  @impl true
  def handle_info({:DOWN, _reference, :process, pid, _type}, pid) do
    {:stop, :normal, pid}
  end
end
