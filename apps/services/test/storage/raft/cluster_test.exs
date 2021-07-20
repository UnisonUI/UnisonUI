defmodule Services.Storage.Raft.ClusterTest do
  use ExUnit.Case

  alias Common.{Events, Service}

  setup_all do
    Application.put_env(:services, :quorum, 2)
  end

  describe "Given an healthy cluster" do
    setup do
      random = :crypto.strong_rand_bytes(10) |> Base.hex_encode32()

      prefix = "unisonui_node_#{random}_"

      nodes =
        LocalCluster.start_nodes(prefix, 3,
          environment: [services: [quorum: 2]],
          applications: [:services]
        )

      nodes |> map(Helpers, :wait_ready, []) |> Enum.each(&assert/1)

      nodes |> map(Consumer, :start_link, [[]]) |> Enum.each(&assert(match?({:ok, _}, &1)))

      [nodes: nodes]
    end

    test "write and read data", %{nodes: nodes} do
      service = %Service.OpenApi{id: "test", name: "test", content: "test"}
      event = %Events.Up{service: service}
      result = {:ok, service}
      writer = Enum.random(nodes)
      assert call(writer, Services.Storage.Raft, :dispatch_events, [[event]]) == :ok

      nodes
      |> map(Helpers, :get_state, [])
      |> Enum.each(&assert(&1 == {:ok, [event]}))

      nodes
      |> map(Services.Storage.Raft, :service, ["test"])
      |> Enum.each(&assert(&1 == result))
    end

    test "having a tolerable partition", %{nodes: nodes} do
      service = %Service.OpenApi{id: "test", name: "test", content: "test"}
      event = %Events.Up{service: service}
      result = {:ok, service}

      {left, right} = Helpers.get_leaders(nodes)

      writer = Enum.random(right)

      map(left, Application, :stop, [:services])
      assert call(writer, Services.Storage.Raft, :dispatch_events, [[event]]) == :ok

      right
      |> Enum.map(fn node ->
        call(node, Helpers, :get_state, [])
      end)
      |> Enum.each(&assert(&1 == {:ok, [event]}))

      right
      |> Enum.map(&call(&1, Services.Storage.Raft, :service, ["test"]))
      |> Enum.each(&assert(&1 == result))

      map(left, Application, :ensure_all_started, [:services])

      nodes |> map(Helpers, :wait_ready, []) |> Enum.each(&assert/1)

      left
      |> Enum.map(&call(&1, Services.Storage.Raft, :service, ["test"]))
      |> Enum.each(&assert(&1 == result))
    end
  end

  describe "Forming a cluster over time" do
    setup do
      random = :crypto.strong_rand_bytes(10) |> Base.hex_encode32()

      prefix = "unisonui_node_#{random}_"
      opts = [environment: [services: [quorum: 2]], applications: [:services]]
      [first] = LocalCluster.start_nodes(prefix, 1, opts)
      Process.sleep(1_000)
      [second] = LocalCluster.start_nodes(prefix <> "2", 1, opts)
      Process.sleep(1_000)
      [third] = LocalCluster.start_nodes(prefix <> "3", 1, opts)
      nodes = [first, second, third]
      nodes |> map(Helpers, :wait_ready, []) |> Enum.each(&assert/1)

      nodes |> map(Consumer, :start_link, [[]]) |> Enum.each(&assert(match?({:ok, _}, &1)))

      [nodes: nodes]
    end

    test "write and read data", %{nodes: nodes} do
      service = %Service.OpenApi{id: "test", name: "test", content: "test"}
      event = %Events.Up{service: service}
      result = {:ok, service}
      writer = Enum.random(nodes)
      call(writer, Services.Storage.Raft, :dispatch_events, [[event]])

      nodes
      |> map(Helpers, :get_state, [])
      |> Enum.each(&assert(&1 == {:ok, [event]}))

      nodes
      |> map(Services.Storage.Raft, :service, ["test"])
      |> Enum.each(&assert(&1 == result))
    end
  end

  defp map(nodes, m, f, a), do: Enum.map(nodes, &call(&1, m, f, a))
  defp call(node, m, f, a), do: :rpc.call(node, m, f, a)
end