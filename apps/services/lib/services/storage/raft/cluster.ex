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

    Logger.info("Bootstrapping Raft")
    {:ok, false, {:continue, :bootstrap}}
  end

  @impl true
  def handle_continue(:bootstrap, _) do
    settings = Application.fetch_env!(:services, :raft)
    quorum = settings[:quorum]

    with {:error, _} <- :ra.restart_server(:default, {:unisonui, node()}) do
      unless quorum_formed?(quorum) do
        {:noreply, false, {:continue, :bootstrap}}
      else
        _ =
          [:this, :visible]
          |> Node.list()
          |> start_or_bootstrap()

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

  defp quorum_formed?(quorum) do
    size =
      [:this, :visible]
      |> Node.list()
      |> length()

    size >= quorum
  end

  defp start_or_bootstrap(nodes) do
    ra_nodes = Enum.map(nodes, fn node -> {:unisonui, node} end)

    case find_leader(nodes, ra_nodes) do
      nil ->
        :ra.start_cluster(
          :default,
          :unisonui,
          @ra_state_machine,
          ra_nodes
        )

      {nodes, leader} ->
        self = {:unisonui, node()}
        _ = :ra.add_member(leader, self)
        :ra.start_server(:default, :unisonui, self, @ra_state_machine, nodes)
    end
  end

  defp find_leader(nodes, ra_nodes) do
    nodes
    |> :rpc.multicall(:ra, :members, [ra_nodes])
    |> elem(0)
    |> Enum.flat_map(fn
      {:ok, nodes, leader} -> [{nodes, leader}]
      _ -> []
    end)
    |> Enum.at(0)
  end
end
