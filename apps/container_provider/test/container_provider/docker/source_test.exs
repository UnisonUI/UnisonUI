defmodule ContainerProvider.Docker.SourceTest do
  use ExUnit.Case, async: true

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

  describe "Handling error" do
    test "the events endpoint failed", %{bypass: bypass} do
      Bypass.expect(bypass, fn conn ->
        assert "GET" == conn.method
        assert "/events" == conn.request_path

        assert URI.encode(~s/since=0&filters={"event":["start","stop"],"type":["container"]}/) ==
                 conn.query_string

        Plug.Conn.resp(conn, 400, "Bad request")
      end)

      start_source("http://localhost:#{bypass.port}")
      refute_receive _, 1_000
    end

    test "could not decode event", %{bypass: bypass} do
      Bypass.expect(bypass, "GET", "/events", fn conn ->
        Plug.Conn.resp(conn, 200, "{}")
      end)

      start_source("http://localhost:#{bypass.port}")
      refute_receive _, 1_000
    end
  end

  test "there is a container which is up and one down", %{bypass: bypass} do
    labels = matching_labels(bypass.port)

    setup_mock(bypass, labels, [
      %{"id" => @id, "status" => "start", "Actor" => %{"Attributes" => labels}},
      %{"id" => @id, "status" => "stop", "Actor" => %{"Attributes" => labels}}
    ])

    start_source("http://localhost:#{bypass.port}")

    up = %Services.Event.Up{
      service: %Services.OpenApi{
        content: "OK",
        id: @id,
        metadata: %Services.Metadata{
          file: "openapi.yaml",
          provider: "container"
        },
        name: @service_name,
        use_proxy: false
      }
    }

    down = %Services.Event.Down{id: @id}
    assert_receive ^up, 1_000

    assert_receive ^down, 1_000
  end

  test "there is a mising label", %{bypass: bypass} do
    labels = %{"unisonui.service-name" => @service_name}

    setup_mock(bypass, labels, [
      %{"id" => @id, "status" => "start", "Actor" => %{"Attributes" => labels}}
    ])

    start_source("http://localhost:#{bypass.port}")

    refute_receive _, 1_000
  end

  test "there is a no labels", %{bypass: bypass} do
    setup_mock(bypass, %{}, [%{"id" => @id, "status" => "start"}])

    start_source("http://localhost:#{bypass.port}")

    refute_receive _, 1_000
  end

  defp start_source(host) do
    start_supervised!({AggregatorStub, self()})
    start_supervised!({ContainerProvider.Docker.Source, host})
  end

  defp setup_mock(bypass, labels, events) do
    agent = start_supervised!({Agent, fn -> events end})

    Bypass.expect(bypass, "GET", "/events", fn conn ->
      event =
        Agent.get_and_update(agent, fn
          [] -> {%{}, []}
          [event | state] -> {event, state}
        end)

      Plug.Conn.resp(conn, 200, Jason.encode!(event))
    end)

    json =
      Jason.encode!(%{
        "Config" => %{"Labels" => labels},
        "NetworkSettings" => %{"Networks" => %{"Bridge" => %{"IPAddress" => "localhost"}}}
      })

    Enum.each(events, fn %{"id" => id} ->
      Bypass.stub(bypass, "GET", "/containers/#{id}/json", fn conn ->
        Plug.Conn.resp(conn, 200, json)
      end)
    end)

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
