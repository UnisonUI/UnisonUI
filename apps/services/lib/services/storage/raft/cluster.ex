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

    with {:error, _} <- :ra.restart_server(:default, self) do
      _ =
        :ra.start_server(
          :default,
          :unisonui,
          self,
          @ra_state_machine,
          nodes
        )
    end

    {:noreply, true}
  end

  @impl true
  def handle_call(:running?, _, running?), do: {:reply, running?, running?}
end
