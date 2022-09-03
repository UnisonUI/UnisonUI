defmodule GitProvider.Git.ServerTest do
  use ExUnit.Case, async: false
  alias GitProvider.LocalGit
  alias GitProvider.Git.{Repository, Server}
  alias Services.Storage.Memory
  alias Services.{Event, Service}

  @duration 50

  @event_down %Event.Down{id: "test:test"}

  @specs ~s/version: "2"
openapi:
  specifications:
    - test2
/
  @grpc_specs ~s/version: "2"
openapi:
  specifications:
    - test
grpc:
  servers:
    - address: 127.0.0.1
      port: 8080
  protobufs:
    "helloworld.proto": {}
/

  defp expected_event_up(id, file \\ nil),
    do: %Event.Up{
      service: %Service.OpenApi{
        content: id,
        id: "test:#{id}",
        metadata: %Service.Metadata{
          file: file || id,
          provider: "git"
        },
        name: "test",
        use_proxy: false
      }
    }

  setup_all do
    Application.put_env(:services, :storage_backend, Services.Storage.Memory)
    Application.put_env(:services, :aggregator, AggregatorStub)
    Application.put_env(:git_provider, :pull_interval, "#{@duration}ms")

    start_supervised(
      {Horde.DynamicSupervisor, [name: GitProvider.Git.DynamicSupervisor, strategy: :one_for_one]}
    )

    :ok
  end

  setup do
    local_git = LocalGit.new()

    repo = %Repository{
      service_name: "test",
      uri: "file://#{local_git.path}",
      name: "test",
      branch: "master"
    }

    on_exit(fn ->
      LocalGit.clean(local_git)
    end)

    start_supervised(Memory.Server)
    [repo: repo, local_git: local_git]
  end

  test "there is a failure with a git command", context do
    repo = %Repository{context.repo | branch: "i-do-not-exist"}
    _ = start_git(repo)
    refute_receive _, 1_000
    stop_git(repo)
  end

  describe "retrieving files from git" do
    test "there is no matching files", context do
      _ = start_git(context.repo)
      refute_receive _, 1_000
      stop_git(context.repo)
    end
  end

  describe "find files" do
    test "no new files", context do
      {_, 0} = LocalGit.commit(context.local_git, "test", "test")
      _ = start_git(context.repo)
      expected_event_up = expected_event_up("test")
      assert_receive ^expected_event_up, 1_000
    end

    test "a new file is present", context do
      {_, 0} = LocalGit.commit(context.local_git, "test", "test")
      _ = start_git(context.repo)

      expected_event_up = expected_event_up("test")
      assert_receive ^expected_event_up, 1_000

      {_, 0} = LocalGit.commit(context.local_git, "test2", "test2")
      {_, 0} = LocalGit.commit(context.local_git, ".unisonui.yaml", @specs)

      assert_receive @event_down, 1_000

      expected_event_up = expected_event_up("test2")
      assert_receive ^expected_event_up, 1_000
    end

    test "a file content has been changed", context do
      {_, 0} = LocalGit.commit(context.local_git, "test", "test")
      _ = start_git(context.repo)

      expected_event_up = expected_event_up("test")
      assert_receive ^expected_event_up, 1_000

      {_, 0} = LocalGit.commit(context.local_git, "test", "test2")

      assert_receive %Event.Changed{
                       service: %Service.OpenApi{
                         content: "test2",
                         id: "test:test",
                         metadata: %Service.Metadata{
                           file: "test",
                           provider: "git"
                         },
                         name: "test",
                         use_proxy: false
                       }
                     },
                     1_000

      stop_git(context.repo)
    end

    test "a file has been deleted", context do
      {_, 0} = LocalGit.commit(context.local_git, "test", "test")
      _ = start_git(context.repo)

      expected_event_up = expected_event_up("test")
      assert_receive ^expected_event_up, 1_000

      {_, 0} = LocalGit.rm(context.local_git, "test")
      assert_receive @event_down, 1_000
      stop_git(context.repo)
    end
  end

  defp start_git(repo) do
    start_supervised!({AggregatorStub, self()})
    start_supervised!({Server, repo})
  end

  defp stop_git(repo), do: stop_supervised!({Git, repo.name})
end
