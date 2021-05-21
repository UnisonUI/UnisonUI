defmodule Database.Mnesia.Sauron do
  @moduledoc false

  use GenServer
  require Logger
  @stores Application.fetch_env!(:database, :stores)
  @all_stores [:schema | @stores]
  @check_interval 1 * 60 * 60 * 1_000
  @impl true
  def init(_) do
    case init?() |> bootstrap_schema() |> log("[init]") do
      :ok ->
        check_dead_nodes()
        {:ok, nil}

      {:aborted, reason} ->
        {:stop, reason}
    end
  end

  def add_mnesia_node(node), do: GenServer.call({:global, __MODULE__}, {:add_node, node})

  defp init?,
    do:
      match?(
        {:atomic, [_ | _]},
        :mnesia.transaction(fn -> :mnesia.table_info(:schema, :disc_copies) end)
      )

  defp bootstrap_schema(false) do
    with _ <- stop_server(),
         :ok <- :mnesia.delete_schema([node()]),
         :ok <- :mnesia.create_schema([node()]),
         :ok <- start_server() do
      :ok
    end
  end

  defp bootstrap_schema(true), do: :ok

  defp start_server, do: :mnesia.start()

  defp stop_server, do: :mnesia.stop()

  def handle_info(:check_dead_nodes, state) do
    Logger.debug("Removing old nodes")
    nodes = :mnesia.system_info(:running_db_nodes) |> MapSet.new()

    with {:ok, replicas} <- replicas(:schema) do
      replicas = MapSet.new(replicas)

      _ =
        replicas
        |> MapSet.new()
        |> MapSet.difference(nodes)
        |> Enum.flat_map(fn node -> Enum.map(@all_stores, &{&1, node}) end)
        |> Enum.each(&remove_node/1)
    else
      error -> log(error, "[Getting replicas]")
    end

    check_dead_nodes()

    {:noreply, state}
  end

  @impl true
  def handle_info(message, state) do
    Logger.warn("Unhandled message in leader: #{inspect(message)}")
    {:noreply, state}
  end

  @impl true
  def terminate(reason, _state) do
    Logger.warn("Terminate leader: #{inspect(reason)}")
    :ok
  end

  @impl true
  def handle_call({:add_node, node}, _from, state) do
    if add_node?(:schema, node) do
      log(:mnesia.change_config(:extra_db_nodes, [node]), "[Change config]")
      log(:mnesia.change_table_copy_type(:schema, node, :disc_copies), "[Copy config]")
    else
      Logger.info("[Sauron] - Node: #{inspect(node)} already in cluster")
    end

    Enum.each(@stores, fn store ->
      if add_node?(store, node) do
        log(store.copy_store(node), "[Add table copy]")
      else
        unless store.initialised?() do
          log(store.init_store(), "[Add init table]")
        else
          log(store.copy_store(node), "[Add table copy]")
        end
      end
    end)

    {:reply, :ok, state}
  end

  defp add_node?(table, node) do
    case replicas(table) do
      {:error, _} -> false
      {:ok, replicas} -> node not in replicas
    end
  end

  defp check_dead_nodes, do: Process.send_after(self(), :check_dead_nodes, @check_interval)

  defp replicas(table) do
    case :mnesia.transaction(fn ->
           case table do
             :schema -> :mnesia.table_info(:schema, :disc_copies)
             store -> store.replicas()
           end
         end) do
      {:aborted, reason} -> {:error, reason}
      {:atomic, replicas} -> {:ok, replicas}
    end
  end

  defp remove_node({table, node}) do
    :mnesia.transaction(fn -> :mnesia.del_table_copy(table, node) end)
    |> log("[Removing #{table} on #{node}]")
  end

  defp log({:error, reason}, context), do: log({:aborted, reason}, context)

  defp log({:aborted, reason} = result, context) do
    Logger.warn("[Sauron] #{context} - Adding node failed: #{inspect(reason)}")
    result
  end

  defp log(result, _context), do: result
end
