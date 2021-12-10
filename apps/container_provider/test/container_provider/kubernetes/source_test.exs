defmodule ContainerProvider.Kubernetes.SourceTest do
  use ExUnit.Case, async: false
  import Mock
  @service_name "test"
  @id "12345"
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

  describe "test" do
    test "test", %{bypass: bypass} do
      labels = matching_labels(bypass.port)

      setup_mock(bypass)

      with_mocks([
        {K8s.Conn, [], from_service_account: fn -> {:ok, nil} end},
        {K8s.Client, [:passthrough],
         list: fn version, endpoint, opts -> passthrough([version, endpoint, opts]) end,
         run: fn _conn, _operation ->
           {:ok,
            %{
              "items" => [
                %{
                  "metadata" => %{
                    "namespace" => "ns",
                    "labels" => labels,
                    "uid" => @id
                  },
                  "specs" => %{"clusterIP" => "localhost"}
                }
              ]
            }}
         end}
      ]) do
        start_source()

        assert_receive %Services.Event.Up{
                         service: %Services.OpenApi{
                           content: "OK",
                           id: "12345",
                           metadata: %Services.Metadata{
                             file: "openapi.yaml",
                             provider: "container"
                           },
                           name: "test",
                           use_proxy: false
                         }
                       },
                       1_000
      end
    end
  end

  defp start_source do
    start_supervised!({AggregatorStub, self()})
    start_supervised!({ContainerProvider.Kubernetes.Source, 1})
  end

  defp setup_mock(bypass) do
    agent = start_supervised!({Agent, fn -> [] end})
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
