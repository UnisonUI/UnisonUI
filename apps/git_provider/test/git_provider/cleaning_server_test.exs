defmodule GitProvider.CleaningServerTest do
  use ExUnit.Case, async: false
  alias GitProvider.{CleaningServer, TestServer}
  alias Services.Storage.Memory
  alias Services.Service.{AsyncApi, Metadata}
  alias Services.Event

  @event_down %Event.Down{id: "test:test"}

  setup_all do
    Application.put_env(:services, :storage_backend, Services.Storage.Memory)
    Application.put_env(:services, :aggregator, AggregatorStub)

    start_supervised(
      {Horde.DynamicSupervisor, [name: GitProvider.Git.DynamicSupervisor, strategy: :one_for_one]}
    )

    :ok
  end

  test "clean unvailable service" do
    start_supervised(Memory.Server)
    start_supervised!({AggregatorStub, self()})
    TestServer.start_child("test_server")

    Services.dispatch_events([
      %Event.Up{
        service: %AsyncApi{
          id: "test:test",
          name: "test",
          content: "",
          metadata: %Metadata{provider: "git", file: "test"}
        }
      },
      %Event.Up{
        service: %AsyncApi{
          id: "test2:test",
          name: "test",
          content: "",
          metadata: %Metadata{provider: "other", file: "test"}
        }
      }
    ])

    start_supervised({CleaningServer, "stub"})
    assert_receive @event_down, 1_000
  end
end
