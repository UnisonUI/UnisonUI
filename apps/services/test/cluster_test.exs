defmodule ClusterTest do
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
      call(writer, Services, :dispatch_event, [event])

      nodes
      |> map(Helpers, :get_state, [])
      |> Enum.each(&assert(&1 == {:ok, [event]}))

      nodes
      |> map(Services, :service, ["test"])
      |> Enum.each(&assert(&1 == result))
    end

    test "having a tolerable partition", %{nodes: nodes} do
      service = %Service.OpenApi{id: "test", name: "test", content: "test"}
      event = %Events.Up{service: service}
      result = {:ok, service}

      {left, right} = Helpers.get_leaders(nodes)

      writer = Enum.random(right)
      # Enum.each(left, &Helpers.disconnect(nodes, &1))
      # map(left, Node, :disconnect, [:"manager@127.0.0.1"])
      map(left,Application,:stop,[:services])
      assert call(writer, Services, :dispatch_event, [event]) == :ok

      right
      |> Enum.map(fn node ->
        call(node, Helpers, :get_state, [])
      end)
      |> Enum.each(&assert(&1 == {:ok, [event]}))

      #
      # left
      # |> Enum.map(&call(&1, Services, :service, ["test"]))
      # |> Enum.each(&assert(&1 == {:error, :not_found}))
      #
      right
      |> Enum.map(&call(&1, Services, :service, ["test"]))
      |> Enum.each(&assert(&1 == result))
      map(left,Application,:ensure_all_started,[:services])


      nodes |> map(Helpers, :wait_ready, []) |> Enum.each(&assert/1)

      left
      |> Enum.map(&call(&1, Services, :service, ["test"]))
      |> Enum.each(&assert(&1 == result))
    end
  end

  defp map(nodes, m, f, a), do: Enum.map(nodes, &call(&1, m, f, a))
  defp call(node, m, f, a), do: :rpc.call(node, m, f, a)
end
