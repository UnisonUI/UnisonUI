defmodule GitProvider.Git.Supervisor do
  @dialyzer :no_match
  use Supervisor
  alias GitProvider.Git.Repository

  @spec start_link() :: Supervisor.on_start()
  def start_link, do: Supervisor.start_link(__MODULE__, nil, name: __MODULE__)

  @impl true
  def init(_),
    do:
      Supervisor.init(
        [
          {Horde.DynamicSupervisor,
           [name: GitProvider.Git.DynamicSupervisor, strategy: :one_for_one, members: :auto]},
          {Task,
           fn ->
             __MODULE__.start_repositories(Application.fetch_env!(:git_provider, :repositories))
           end}
        ],
        strategy: :rest_for_one
      )

  def start_repositories(repositories) do
    repositories
    |> Stream.filter(fn
      location_or_path when is_binary(location_or_path) ->
        location_or_path |> URI.parse() |> Map.get(:scheme) != nil

      %{"location" => location} ->
        location |> URI.parse() |> Map.get(:scheme) != nil

      _ ->
        false
    end)
    |> Stream.map(fn
      location when is_binary(location) ->
        service_name = location |> URI.parse() |> Map.get(:path) |> String.trim("/")
        %Repository{service_name: service_name, uri: location, branch: "master"}

      %{"location" => location} = repository ->
        service_name = location |> URI.parse() |> Map.get(:path) |> String.trim("/")
        branch = Map.get(repository, "branch", "master")

        %Repository{
          service_name: service_name,
          uri: location,
          branch: branch
        }
    end)
    |> Enum.each(&start_git/1)
  end

  @spec start_git(repository :: GitProvider.Git.Repository.t()) ::
          Supervisor.on_start_child()
  def start_git(repository) do
    unless provider_enabled() do
      :ignore
    else
      repository = %Repository{
        repository
        | name:
            repository.uri
            |> URI.parse()
            |> Map.get(:path)
            |> String.trim_leading("/")
      }
      GitProvider.Git.Server.start_child(repository)
    end
  end

  defp provider_enabled, do: Application.fetch_env!(:git_provider, :enabled)
end
