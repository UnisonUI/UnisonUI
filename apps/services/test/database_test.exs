defmodule DatabaseTest do
  use ExUnit.ClusteredCase
  alias Database.Schema.{Events, State}

  scenario "Given an healthy cluster",
    cluster_size: 3,
    stdout: :stdio,
    boot_timeout: 15_000 do
    node_setup do
      {:ok, _} = Application.ensure_all_started(:database)
    end

    test "write and read data", %{cluster: cluster} do
      result = {:ok, %Events{id: 1, event: "event"}}
      writer = Cluster.random_member(cluster)
Process.sleep(10_000)
    end

    # test "having a tolerable partition", %{cluster: cluster} do
      # data = %State{id: 1, state: "event"}
      # result = {:ok, data}
      # Cluster.map(cluster, &Database.wait_ready/0) |> Enum.each(&(&1 == :ok))
#
      # writer =
        # cluster
        # |> Cluster.random_member()
        # |> Cluster.call(fn -> :global.whereis_name(Database.Mnesia.Sauron) |> node() end)
#
      # {left, others} = Cluster.members(cluster) |> Enum.reject(&(&1 == writer)) |> Enum.split(1)
#
      # Cluster.partition(cluster, [[writer | left], others])
#
      # assert Cluster.call(writer, fn -> Database.transaction(fn -> Database.write(data) end) end) ==
               # result
#
      # Cluster.each(cluster, fn -> :mnesia.info end)
      # Cluster.map(cluster, fn -> Database.transaction(fn -> Database.read(State, 1) end) end)
      # |> Enum.each(&assert(&1 == result))
    # end
  end
end
