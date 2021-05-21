defmodule Database.Mnesia.Migration do
  use GenServer
  require Logger

  def start_link,
    do: GenServer.start_link(__MODULE__, :ok, name: {:global, __MODULE__})

  @impl true
  def init(:ok) do
    :global.whereis_name(__MODULE__) |> send(:migrate)

    {:ok,
     Application.get_env(:database, :stores) |> Enum.reject(&(&1 == Database.Schema.Migration))}
  end

  @impl true
  def handle_info(:migrate, stores) do
    _ = :mnesia.wait_for_tables([Database.Schema.Migration], 60_000)

    pids =
      stores
      |> Enum.map(fn store ->
        spawn_monitor(fn -> migrate(store) end)
      end)

    {:noreply, pids}
  end

  @impl true
  def handle_info({:DOWN, ref, :process, pid, _reason}, state),
    do: {:noreply, state |> Enum.reject(&match?({^pid, ^ref}, &1))}

  defp migrate(store) do
    info = Database.transaction(fn -> Database.read(Database.Schema.Migration, store) end)

    case info do
      {:ok, nil} ->
        Database.transaction(fn ->
          Database.write(%Database.Schema.Migration{store: store, store_version: store.version()})
        end)

      {:ok, %Database.Schema.Migration{store_version: version}} ->
        if store.version() > version do
          Logger.info("[Migration - #{store}] Migrating from #{version} to #{store.version()}")
          current_store = store.from_version(version)

          old_index_mapset = MapSet.new(current_store.index() || [])
          new_index_mapset = MapSet.new(store.index() || [])

          removed =
            old_index_mapset
            |> MapSet.difference(new_index_mapset)
            |> MapSet.to_list()

          insert =
            new_index_mapset
            |> MapSet.difference(old_index_mapset)
            |> MapSet.to_list()

          Logger.info("[Migration - #{store}] Removing indexes #{inspect(removed)}")
          removed |> Enum.each(&:mnesia.del_table_index(store, &1))

          Logger.info("[Migration - #{store}] Transforming table")

          _ =
            :mnesia.transform_table(
              store,
              &current_store.migration/1,
              current_store.next().attributes()
            )

          Logger.info("[Migration - #{store}] Adding indexes #{inspect(insert)}")
          insert |> Enum.each(&:mnesia.add_table_index(store, &1))

          Database.transaction(fn ->
            Database.write(%Database.Schema.Migration{
              store: store,
              store_version: store.version()
            })
          end)
        else
          Logger.info("[Migration - #{store}] Migration not required")
        end

      {:error, reason} ->
        Logger.warn("[Migration - #{store}] error #{inspect(reason)}")
    end
  end
end
