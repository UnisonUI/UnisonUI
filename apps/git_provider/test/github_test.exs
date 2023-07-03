defmodule GitProvider.GithubTest do
  alias GitProvider.Github.Client
  alias GitProvider.GraphQL.Settings
  alias GitProvider.GraphQL.Data.Project
  alias GitProvider.Git.{Repository, Supervisor}
  use ExUnit.Case
  import Mock

  setup_all do
    [
      settings: %Settings{
        api_token: "",
        api_uri: "https://api.github.com/graphql",
        polling_interval: 5,
        patterns: ["keep.*"]
      }
    ]
  end

  test "retrieving new repository", context do
    parent = self()

    with_mocks [
      {Client, [],
       [
         list_projects: fn _, _ ->
           {:ok,
            [
              %Project{name: "keep1", branch: "master", url: "http://localhost"},
              %Project{name: "ignore", branch: "master", url: "http://localhost"}
            ]}
         end
       ]},
      {Supervisor, [],
       [
         start_git: fn %Repository{service_name: name} ->
           send(parent, name)
           {:ok, name}
         end
       ]}
    ] do
      _ = start_supervised!({GitProvider.GraphQL, {Client, context.settings}})
      assert_receive "keep1"
    end
  end
end
