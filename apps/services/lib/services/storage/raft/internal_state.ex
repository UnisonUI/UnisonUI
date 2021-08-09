defmodule Services.Storage.Raft.InternalState do
  alias Services.State

  @behaviour :ra_machine

  @task Services.TaskSupervisor
  @snapshot_every 10

  @impl true
  def init(_), do: State.new()

  @impl true
  def apply(%{index: index}, {:events, events}, state) do
    {state, events} =
      Enum.reduce(events, {state, []}, fn event, {state, events} ->
        {state, new_events} = State.reduce(state, event)
        {state, events ++ new_events}
      end)

    {state, :ok, side_effets(index, events, state)}
  end

  defp dispatch_events(events),
    do:
      {:mod_call, Task.Supervisor, :start_child,
       [
         @task,
         fn ->
           Node.list([:visible, :this])
           |> Enum.each(&:rpc.call(&1, Services.Aggregator, :append_events, [events]))
         end
       ]}

  defp side_effets(index, events, state) do
    side_effets = [dispatch_events(events)]

    if rem(index, @snapshot_every) == 0,
      do: [{:release_cursor, index, state} | side_effets],
      else: side_effets
  end
end
