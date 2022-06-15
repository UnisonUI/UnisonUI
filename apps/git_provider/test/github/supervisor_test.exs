defmodule GitProvider.Github.SupervisorTest do
  use ExUnit.Case, async: true
  alias GitProvider.Github.Supervisor

  describe "init/1" do
    test "without api_token" do
      settings =
        Application.get_env(:git_provider, :github)
        |> Keyword.put(:api_token, "")

      Application.put_env(:git_provider, :github, settings)
      assert match?({:ok, {_, []}}, Supervisor.init(:ok))
    end

    test "with api_token" do
      settings =
        Application.get_env(:git_provider, :github)
        |> Keyword.put(:api_token, "test")
        |> Keyword.put(:polling_interval, "1h")

      Application.put_env(:git_provider, :github, settings)

      assert match?(
               {:ok,
                {_,
                 [
                   %{
                     id: Finch,
                     start: {Finch, :start_link, [[name: NeuroFinch]]},
                     type: :supervisor
                   },
                   %{
                     id: GitProvider.Github,
                     start:
                       {GitProvider.Github, :start_link,
                        [
                          %GitProvider.Github.Settings{
                            api_token: "test",
                            api_uri: "https://api.github.com/graphql",
                            patterns: [],
                            polling_interval: 3_600_000
                          }
                        ]}
                   }
                 ]}},
               Supervisor.init(:ok)
             )
    end

    test "using polling_interval as a integer" do
      settings =
        Application.get_env(:git_provider, :github)
        |> Keyword.put(:api_token, "test")
        |> Keyword.put(:polling_interval, 1_000)

      Application.put_env(:git_provider, :github, settings)

      assert match?(
               {:ok,
                {_,
                 [
                   %{
                     id: Finch,
                     start: {Finch, :start_link, [[name: NeuroFinch]]},
                     type: :supervisor
                   },
                   %{
                     id: GitProvider.Github,
                     start:
                       {GitProvider.Github, :start_link,
                        [
                          %GitProvider.Github.Settings{
                            api_token: "test",
                            api_uri: "https://api.github.com/graphql",
                            patterns: [],
                            polling_interval: 1_000
                          }
                        ]}
                   }
                 ]}},
               Supervisor.init(:ok)
             )
    end
  end
end
