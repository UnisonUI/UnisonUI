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

  defp nodes,
    do: Node.list([:visible, :this]) |> Enum.reject(&MapSet.member?(ignore_servers(), &1))

  defp check_quorum, do: length(nodes()) >= quorum()

  defp interval, do: Application.get_env(:services, :interval_node_down_ms, 1_000)
  defp ignore_servers, do: Application.get_env(:services, :ignore_servers, []) |> MapSet.new()

  @impl true
  def init(:ok) do
    _ = :net_kernel.monitor_nodes(true, node_type: :visible)

    if Node.list() == [] and quorum() == 1 do
      actions = [{:next_event, :internal, :start_cluster}]
      {:ok, :starting, [{:unisonui, node()}], actions}
    else
      actions = [{{:timeout, :waiting_for_quorum}, 1_000, 15}]
      {:ok, :starting, :ok, actions}
    end
  end

  def starting({:call, from}, :running?, _state),
    do: {:keep_state_and_data, [{:reply, from, false}]}

  def starting(:internal, :start_cluster, nodes) do
    :global.trans({:unisonui, :bootstrap}, fn ->
      :ra.start_cluster(:unisonui, {:module, Services.State, %{}}, nodes)
    end)

    {:next_state, :running, :ok}
  end

  def starting({:timeout, :waiting_for_quorum}, 0, _state),
    do: {:stop, {:error, :cluster_not_formed}}

  def starting({:timeout, :waiting_for_quorum}, rem, _state) do
    if check_quorum() do
      bootstrap_or_start_server()
    else
      actions = [{{:timeout, :waiting_for_quorum}, 1_000, rem - 1}]
      {:keep_state_and_data, actions}
    end
  end

  def starting(event_type, event_content, data) do
    handle_event(event_type, event_content, :starting, data)
  end

  defp bootstrap_or_start_server do
    nodes = Enum.reject(nodes(), &(&1 == node()))
    {succeed, _failed} = :rpc.multicall(nodes, :ra, :overview, [])

    maybe_leader =
      Enum.flat_map(succeed, fn
        %{node: node, servers: %{unisonui: %{state: :leader}}} -> [node]
        _ -> []
      end)

    nodes = Enum.map(nodes(), &{:unisonui, &1})
    self = {:unisonui, node()}

    case maybe_leader do
      [] ->
        actions = [{:next_event, :internal, :start_cluster}]

        {:next_state, :starting, nodes, actions}

      [leader] ->
        with {:error, _} <- :ra.restart_server(self) do
          _ =
            :ra.start_server(
              :unisonui,
              {:unisonui, nodes()},
              {:module, Services.State, %{}},
              Enum.reject(nodes, &(&1 == self))
            )

          _ = :rpc.call(leader, :ra, :add_member, [:unisonui, self])
          {:next_state, :running, :ok}
        end
    end
  end

  def running({:call, from}, :running?, _state),
    do: {:keep_state_and_data, [{:reply, from, true}]}

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
    Logger.debug(msg)
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
