defmodule GitProvider.GitTest do
  use ExUnit.Case, async: false
  alias GitProvider.LocalGit
  alias GitProvider.Git.{Repository, Server}

  import Mox
  @service_mock Services.Mock
  @duration 5

  @specs "specifications:
    - test2
  "
  @grpc_specs ~s/version: "2"
  openapi:
    specifications:
      - test
  grpc:
    servers:
      - address: 127.0.0.1
        port: 8080
  protobufs:
    "helloworld.proto": {}assert_receive
  /

  setup_all do
    Application.put_env(:services, :storage_backend, Services.Mock)
    Application.put_env(:git_provider, :pull_interval, "#{@duration}ms")

    start_supervised!(
      {Horde.DynamicSupervisor, [name: GitProvider.Git.DynamicSupervisor, strategy: :one_for_one]}
    )

    :ok
  end

  setup :set_mox_from_context
  setup :verify_on_exit!

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

    [repo: repo, local_git: local_git]
  end

  test "there is a failure with a git command", context do
    expect(@service_mock, :dispatch_events, 0, fn _ -> :ok end)
    repo = %Repository{context.repo | branch: "i-do-not-exist"}
    _ = start_git(repo)

    Process.sleep(@duration)
    stop_git(repo)
  end

  describe "retrieving files from git" do
    test "there is no matching files", context do
      expect(@service_mock, :dispatch_events, 0, fn _ -> :ok end)
      _ = start_git(context.repo)

      Process.sleep(@duration)
      stop_git(context.repo)
    end
  end

  describe "find files" do
    test "no new files", context do
      parent = self()

      stub(@service_mock, :dispatch_events, fn [e] ->
        send(parent, e)
        :ok
      end)

      {_, 0} = LocalGit.commit(context.local_git, "test", "test")
      _ = start_git(context.repo)

      assert_receive %Common.Events.Up{
        service: %Common.Service.OpenApi{
          content: "test",
          id: "test:test",
          metadata: %Common.Service.Metadata{
            file: "test",
            provider: ""
          },
          name: "test",
          use_proxy: false
        }
      }

      stop_git(context.repo)
    end

    test "a new file is present", context do
      parent = self()

      @service_mock
      |> stub(:alive?, fn -> true end)
      |> stub(:dispatch_events, fn events ->
        IO.inspect(events)
        Enum.each(events, &send(parent, &1))
      end)

      {_, 0} = LocalGit.commit(context.local_git, "test", "test")
      _ = start_git(context.repo)

      res =
        receive do
          m -> m
        after
          1_000 -> :timeout
        end

      assert res == %Common.Events.Up{
               service: %Common.Service.OpenApi{
                 content: "test",
                 id: "test:test",
                 metadata: %Common.Service.Metadata{
                   file: "test",
                   provider: ""
                 },
                 name: "test",
                 use_proxy: false
               }
             }

      {_, 0} = LocalGit.commit(context.local_git, "test2", "test2")
      {_, 0} = LocalGit.commit(context.local_git, ".unisonui.yaml", @specs)

      res =
        receive do
          m -> m
        after
          1_000 -> :timeout
        end

      assert res == %Common.Events.Down{id: "test:test"}

      res =
        receive do
          m -> m
        after
          1_000 -> :timeout
        end

      assert res == %Common.Events.Up{
               service: %Common.Service.OpenApi{
                 content: "test2",
                 id: "test:test2",
                 metadata: %Common.Service.Metadata{
                   file: "test2",
                   provider: ""
                 },
                 name: "test",
                 use_proxy: false
               }
             }
    end

    test "a file content has been changed", context do
      parent = self()

      stub(@service_mock, :dispatch_events, fn [e] ->
        send(parent, e)
      end)

      {_, 0} = LocalGit.commit(context.local_git, "test", "test")
      _ = start_git(context.repo)

      assert_receive %Common.Events.Up{
        service: %Common.Service.OpenApi{
          content: "test",
          id: "test:test",
          metadata: %Common.Service.Metadata{
            file: "test",
            provider: ""
          },
          name: "test",
          use_proxy: false
        }
      }

      {_, 0} = LocalGit.commit(context.local_git, "test", "test2")

      assert_receive %Common.Events.Up{
        service: %Common.Service.OpenApi{
          content: "test",
          id: "test:test",
          metadata: %Common.Service.Metadata{
            file: "test2",
            provider: ""
          },
          name: "test",
          use_proxy: false
        }
      }

      stop_git(context.repo)
    end

    test "a file has been deleted", context do
      parent = self()

      stub(@service_mock, :dispatch_events, fn [e] ->
        send(parent, e)
      end)

      {_, 0} = LocalGit.commit(context.local_git, "test", "test")
      _ = start_git(context.repo)

      assert_receive %Common.Events.Up{
        service: %Common.Service.OpenApi{
          content: "test",
          id: "test:test",
          metadata: %Common.Service.Metadata{
            file: "test",
            provider: ""
          },
          name: "test",
          use_proxy: false
        }
      }

      {_, 0} = LocalGit.rm(context.local_git, "test")
      assert_receive %Common.Events.Down{id: "test:test"}
      stop_git(context.repo)
    end
  end

  defp start_git(repo), do: start_supervised!({Server, repo})
  defp stop_git(repo), do: stop_supervised!("Git_#{repo.name}")
end
