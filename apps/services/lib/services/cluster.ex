defmodule Services.Cluster do
  @moduledoc """
  FSM to bootstrap mnesia cluster.
  """
  use GenStateMachine, callback_mode: [:state_functions, :state_enter]
  require Logger

  def start_link(_),
    do: GenStateMachine.start_link(__MODULE__, :ok, name: __MODULE__)

  def running?, do: GenStateMachine.call(__MODULE__, :running?)
  defp quorum, do: Application.get_env(:services, :quorum, 1)

  defp nodes, do: Node.list([:visible, :this]) |> Enum.reject(&(&1 == :"manager@127.0.0.1"))

  defp check_quorum, do: length(nodes()) >= quorum()

  defp interval, do: Application.get_env(:services, :interval_node_down_ms, 1_000)

  @impl true
  def init(:ok) do
    _ = :net_kernel.monitor_nodes(true, node_type: :visible)

    if Node.list() == [] and quorum() == 1 do
      actions = [{:next_event, :internal, :start_cluster}]
      {:ok, :starting, [{:unisonui, node()}], actions}
    else
      actions = [{:next_event, :internal, :check_quorum}]
      {:ok, :starting, :ok, actions}
    end
  end

  def starting({:call, from}, :running?, _state),
    do: {:keep_state_and_data, [{:reply, from, false}]}

  def starting(:internal, :start_cluster, nodes) do
    case :ra.start_cluster(:unisonui, {:module, Services.State, %{}}, nodes) do
      {:ok, _, _} ->
        {:next_state, :running, :ok}

      _ ->
        actions = [1_000]
        {:keep_state_and_data, actions}
    end
  end

  def starting(:internal, :check_quorum, _) do
    if check_quorum() do
      actions = [{:next_event, :internal, :start_cluster}]

      {:next_state, :starting, nodes() |> Enum.map(&{:unisonui, &1}), actions}
    else
      {:next_state, :waiting_for_quorum, :ok}
    end
  end

  def starting(event_type, event_content, data) do
    handle_event(event_type, event_content, :starting, data)
  end

  def waiting_for_quorum({:call, from}, :running?, _state),
    do: {:keep_state_and_data, [{:reply, from, false}]}

  def waiting_for_quorum(event_type, event_content, data) do
    handle_event(event_type, event_content, :waiting_for_quorum, data)
  end

  def running({:call, from}, :running?, _state),
    do: {:keep_state_and_data, [{:reply, from, true}]}

  def running(:info, {:nodeup, node, _}, _state) do
    _ = :ra.add_member(:unisonui, {:unison, node})
    :keep_state_and_data
  end

  def running(:info, {:nodedown, node, _}, _state) do
    actions = [{{:timeout, :still_down}, interval(), node}]
    {:keep_state_and_data, actions}
  end

  def running({:timeout, :still_down}, node, _state) do
    unless node in Node.list([:visible]) do
      _ = :ra.remove_member(:unisonui, {:unisonui, node})
    end

    :keep_state_and_data
  end

  def running(event_type, event_content, data) do
    handle_event(event_type, event_content, :running, data)
  end

  def handle_event(:enter, from, to, data) do
    msg = "FSM #{inspect(data)} transitionned from #{from} to #{to}"
    Logger.info(msg)
    :keep_state_and_data
  end

  @impl true
  def handle_event(event_type, event_content, state, data) do
    msg =
      "Receive unexpected message: #{inspect(event_type)}, #{inspect(event_content)},#{inspect(state)}, #{inspect(data)}"

    Logger.warn(msg)

    :keep_state_and_data
  end

  @impl true
  def terminate(reason, _state, _data) do
    Logger.info("Terminated: #{inspect(reason)}")
    :ok
  end
end
