defmodule Services.Cluster do
  @moduledoc """
  FSM to bootstrap mnesia cluster.
  """
  use GenStateMachine, callback_mode: [:state_functions, :state_enter]
  require Logger

  def start_link(_),
    do: GenStateMachine.start_link(__MODULE__, :ok, name: __MODULE__)

  defp quorum, do: Application.get_env(:services, :quorum, 1)

  defp check_quorum, do: length(Node.list([:visible, :this])) >= quorum()

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

      {:next_state, :starting, Node.list([:visible, :this]) |> Enum.map(&{:unisonui, &1}),
       actions}
    else
      {:next_state, :waiting_for_quorum, :ok}
    end
  end

  def starting(event_type, event_content, data) do
    handle_event(event_type, event_content, :starting, data)
  end

  def waiting_for_quorum(event_type, event_content, data) do
    handle_event(event_type, event_content, :waiting_for_quorum, data)
  end

  def running(event_type, event_content, data) do
    handle_event(event_type, event_content, :running, data)
  end

  @impl true
  def handle_event(:cast, {:notify, pid}, _state, _data), do: {:keep_state, pid}

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
