defmodule Consumer do
  use GenStage
  def start_link(_), do: GenStage.start_link(__MODULE__, :ok, name: __MODULE__)
  @impl true
  def init(_), do: {:consumer, [], subscribe_to: [Services.Aggregator]}

  def get_state, do: GenStage.call(__MODULE__, :get_state)
  def reset_state, do: GenStage.cast(__MODULE__, :reset_state)

  @impl true
  def handle_events(events, _from, state), do: {:noreply, [], events ++ state}

  @impl true
  def handle_call(:get_state, _from, state), do: {:reply, state, [], state}

  @impl true
  def handle_cast(:reset_state, _state), do: {:noreply, [], []}
end
