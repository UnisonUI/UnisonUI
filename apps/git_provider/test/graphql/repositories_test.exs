defmodule GitProvider.GraphQL.RepositoriesTest do
  alias GitProvider.Git.Repository
  alias GitProvider.GraphQL.Repositories
  alias GitProvider.GraphQL.Data.Project
  use ExUnit.Case, async: true

  describe "matches_new_repositories/4" do
    test "find new repositories" do
      current = %Repositories{repositories: MapSet.new(["test"])}

      projects = [
        %Project{name: "test", url: "http://test", branch: "master"},
        %Project{name: "test2", url: "http://test2", branch: "master"},
        %Project{name: "ignore", url: "http://ignore", branch: "master"}
      ]

      matching_repositories = [~r/test.*/]

      result =
        Repositories.match_new_repositories(current, projects, matching_repositories, "token")

      assert result == [
               %Repository{service_name: "test2", uri: "http://token@test2", branch: "master"}
             ]
    end
  end

  describe "update/2" do
    test "update repositories" do
      current = %Repositories{repositories: MapSet.new(["test"])}
      result = Repositories.update(current, ["test", "test2"])
      assert result == %Repositories{repositories: MapSet.new(["test", "test2"])}
    end
  end
end
