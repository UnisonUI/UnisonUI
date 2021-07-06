defmodule GitProvider.Git.SupervisorTest do
  alias GitProvider.Git.{Repository, Supervisor}
  use ExUnit.Case, async: true

  import Mox

  setup_all do
    Application.put_env(:services, :behaviour, Services.Mock)
    :ok
  end

  setup :verify_on_exit!

  describe "start_repositories/0" do
    setup do
      Application.put_env(:git_provider, :enabled, true)
      :ok
    end

    test "start git repositories" do
      Application.put_env(:git_provider, :repositories, [
        "http://localhost/repo",
        "some_path/",
        %{"location" => "some_path/", "branch" => "test"},
        %{"location" => "http://localhost/repo", "branch" => "test"},
        42
      ])

      Services.Mock |> expect(:dispatch_events, 0, fn _ -> :ok end)
      _ = start_supervisor()

      Supervisor.start_repositories(Application.fetch_env!(:git_provider, :repositories))

      stop_supervisor()
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
      stop_supervised!(GitProvider.Git.Supervisor)
    end

    test "provider enabled", context do
      _ = start_supervisor()
      Services.Mock |> expect(:dispatch_events, 0, fn _ -> :ok end)
      Application.put_env(:git_provider, :enabled, true)
      result = Supervisor.start_git(context.repository)
      assert match?({:ok, _}, result)
      stop_supervisor()
    end
  end

  defp start_supervisor,
    do: start_supervised!(%{id: GitProvider.Git.Supervisor, start: {Supervisor, :start_link, []}})

  defp stop_supervisor, do: stop_supervised!(GitProvider.Git.Supervisor)
end
