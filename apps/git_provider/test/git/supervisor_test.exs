defmodule GitProvider.Git.SupervisorTest do
  alias GitProvider.Git.{Repository, Supervisor}
  use ExUnit.Case, async: false

  import Mock

  describe "start_repositories/0" do
    setup do
      Application.put_env(:git_provider, :enabled, true)
      :ok
    end

    test "start git repositories" do
      parent = self()

      with_mock Services,
        dispatch_events: fn _ ->
          send(parent, :event)
          :ok
        end do
        Application.put_env(:git_provider, :repositories, [
          "http://localhost/repo",
          "some_path/",
          [location: "some_path/", branch: "test"],
          [location: "http://localhost/repo", branch: "test"],
          42
        ])

        _ = start_supervisor()

        Supervisor.start_repositories(Application.fetch_env!(:git_provider, :repositories))
        refute_receive :event
      end
    end
  end

  describe "start_git/1" do
    setup do
      [repository: %Repository{uri: "http://localhost/repo"}]
    end

    test "provider disabled", context do
      _ = start_supervisor()

      Application.put_env(:git_provider, :enabled, false)
      result = Supervisor.start_git(context.repository)
      assert result == :ignore
    end

    test "provider enabled", context do
      parent = self()

      _ = start_supervisor()

      with_mock Services,
        dispatch_events: fn _ ->
          send(parent, :event)
          :ok
        end do
        Application.put_env(:git_provider, :enabled, true)
        result = Supervisor.start_git(context.repository)
        refute_receive :event
        assert match?({:ok, _}, result)
      end
    end
  end

  defp start_supervisor,
    do: start_supervised(%{id: GitProvider.Git.Supervisor, start: {Supervisor, :start_link, []}})
end
