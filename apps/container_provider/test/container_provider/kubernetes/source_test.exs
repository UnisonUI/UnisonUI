defmodule ContainerProvider.Kubernetes.SourceTest do
  use ExUnit.Case, async: false
  alias Services.{Event, Service}

  import Mock
  @service_name "test"
  @id1 "ns1_1"
  @id2 "ns2_1"
  setup_all do
    Application.put_env(:services, :storage_backend, Services.Storage.Memory)
    Application.put_env(:services, :aggregator, AggregatorStub)
    :ok
  end

  setup do
    bypass = Bypass.open()
    start_supervised!(Services.Storage.Memory.Server)
    {:ok, bypass: bypass}
  end

  describe "Handle error" do
    test "Kubernetes sources couldn't start" do
      with_mock K8s.Conn, from_service_account: fn -> {:error, "some error"} end do
        assert match?(
                 {:error, {:normal, _}},
                 start_supervised({ContainerProvider.Kubernetes.Source, 1})
               )
      end
    end

    test "failed to list services" do
      with_mocks [
        mock_k8s_connection(),
        {K8s.Client, [:passthrough],
         list: fn version, endpoint, opts -> passthrough([version, endpoint, opts]) end,
         run: fn _conn, _operation ->
           {:error, %K8s.Middleware.Error{error: "error"}}
         end}
      ] do
        start_source()
        refute_receive _, 1_000
      end
    end
  end

  test "Handling new services", %{bypass: bypass} do
    labels = matching_labels(bypass.port)

    items = [
      %{
        "metadata" => %{
          "namespace" => "ns1",
          "labels" => labels,
          "uid" => @id1
        },
        "specs" => %{"clusterIP" => "localhost"}
      },
      %{
        "metadata" => %{
          "namespace" => "ns2",
          "labels" => labels,
          "uid" => @id2
        },
        "specs" => %{"clusterIP" => "localhost"}
      }
    ]

    agent = setup_mock(bypass, items)

    with_mocks([mock_k8s_connection(), mock_k8s_run(agent)]) do
      start_source()

      up = %Event.Up{
        service: %Service.OpenApi{
          content: "OK",
          id: @id1,
          metadata: %Service.Metadata{
            file: "openapi.yaml",
            provider: "container"
          },
          name: "test",
          use_proxy: false
        }
      }

      assert_receive ^up, 1_000

      up = %Event.Up{
        service: %Service.OpenApi{
          content: "OK",
          id: @id2,
          metadata: %Service.Metadata{
            file: "openapi.yaml",
            provider: "container"
          },
          name: "test",
          use_proxy: false
        }
      }

      assert_receive ^up, 1_000
      remove_item(agent, "ns1_1")
      down = %Services.Event.Down{id: @id1}

      assert_receive ^down, 1_000
    end
  end

  defp mock_k8s_connection, do: {K8s.Conn, [], from_service_account: fn -> {:ok, nil} end}

  defp mock_k8s_run(agent) do
    {K8s.Client, [:passthrough],
     list: fn version, endpoint, opts -> passthrough([version, endpoint, opts]) end,
     run: fn _conn, _operation ->
       items =
         Agent.get_and_update(agent, fn
           {[], items} ->
             {items, {[], items}}

           {[item | tail], items} ->
             items = [item | items]
             {items, {tail, items}}
         end)

       {:ok, %{"items" => items}}
     end}
  end

  defp remove_item(agent, id) do
    Agent.update(agent, fn {to_send, sent} ->
      {to_send, Enum.reject(sent, fn item -> match?(%{"metadata" => %{"uid" => ^id}}, item) end)}
    end)
  end

  defp start_source do
    start_supervised!({AggregatorStub, self()})
    start_supervised!({ContainerProvider.Kubernetes.Source, 1})
  end

  defp setup_mock(bypass, items) do
    agent = start_supervised!({Agent, fn -> {items, []} end})
    Bypass.stub(bypass, "GET", "/openapi.yaml", fn conn -> Plug.Conn.resp(conn, 200, "OK") end)
    agent
  end

  defp matching_labels(port),
    do: %{
      "unisonui.service-name" => @service_name,
      "unisonui.openapi.port" => to_string(port),
      "unisonui.openapi.path" => "/openapi.yaml"
    }
end
