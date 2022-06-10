defmodule Services.Storage.Raft.Cluster do
  @moduledoc false
  use GenServer
  require Logger

  @ra_state_machine {:module, Services.Storage.Raft.InternalState, %{}}

  def start_link(_),
    do: GenServer.start_link(__MODULE__, :ok, name: __MODULE__)

  def running?, do: GenServer.call(__MODULE__, :running?)
  @impl true
  @spec init(any) :: {:stop, :nodes_required} | {:ok, false, {:continue, :bootstrap}}
  def init(_) do
    :ok = :ra.start()
    settings = Application.fetch_env!(:services, :raft)

    case settings[:nodes] do
      nodes when is_list(nodes) ->
        Logger.info("Bootstrapping Raft")
        {:ok, false, {:continue, :bootstrap}}

      _ ->
        {:stop, :nodes_required}
    end
  end

  @impl true
  def handle_continue(:bootstrap, _) do
    settings = Application.fetch_env!(:services, :raft)

    nodes = Enum.map(settings[:nodes], fn node -> {:unisonui, String.to_atom(node)} end)

    self = {:unisonui, node()}

    ra_nodes =
      case nodes do
        [_ | _] = nodes -> nodes
        _ -> [self]
      end

    with {:error, _} <- :ra.restart_server(:default, self) do
      unless quorum_formed?(nodes) do
        {:noreply, false, {:continue, :bootstrap}}
      else
        _ =
          :ra.start_cluster(
            :default,
            :unisonui,
            @ra_state_machine,
            ra_nodes
          )

        Logger.info("Raft bootstrapped")
        {:noreply, true}
      end
    else
      _ ->
        Logger.info("Raft bootstrapped")
        {:noreply, true}
    end
  end

  @impl true
  def handle_call(:running?, _, running?), do: {:reply, running?, running?}

  defp quorum_formed?(nodes) do
    quorum = div(length(nodes), 2) + 1
    connected_nodes = Node.list([:this, :visible]) |> MapSet.new()
    nodes = MapSet.new(nodes)
    size = MapSet.difference(connected_nodes, nodes) |> MapSet.size()
    size >= quorum
  end
end
