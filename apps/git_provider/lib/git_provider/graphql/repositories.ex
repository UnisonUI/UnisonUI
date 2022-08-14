defmodule GitProvider.GraphQL.Repositories do
  alias GitProvider.GraphQL.Data.Project
  alias GitProvider.Git.Repository

  @type t :: %__MODULE__{repositories: MapSet.t(Repository.t())}
  defstruct repositories: MapSet.new()

  @spec new() :: t()
  def new, do: %__MODULE__{}

  @spec match_new_repositories(
          current :: t(),
          projects :: [Project.t()],
          patterns :: [Regex.t()],
          token :: String.t()
        ) :: [Repository.t()]
  def match_new_repositories(
        %__MODULE__{repositories: current},
        projects,
        patterns,
        token
      ) do
    projects
    |> Stream.filter(fn %Project{name: name} ->
      Enum.any?(patterns, &Regex.match?(&1, name))
    end)
    |> Stream.reject(fn %Project{name: name} ->
      MapSet.member?(current, name)
    end)
    |> Enum.into([], fn %Project{name: name, url: url, branch: branch} ->
      url_with_authority =
        url |> URI.parse() |> Map.update!(:userinfo, fn _ -> token end) |> URI.to_string()

      %Repository{service_name: name, uri: url_with_authority, branch: branch}
    end)
  end

  @spec update(current :: t(), repositories :: [Repository.t()]) :: t()
  def update(%__MODULE__{repositories: current}, repositories),
    do: %__MODULE__{repositories: Enum.reduce(repositories, current, &MapSet.put(&2, &1))}
end
