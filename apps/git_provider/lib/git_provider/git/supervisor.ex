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
    |> Stream.flat_map(fn
      repository when is_list(repository) ->
        with location when not is_nil(location) <- repository[:location],
             uri <- URI.parse(repository[:location]),
             scheme when not is_nil(scheme) <- uri.scheme do
          service_name = String.trim(uri.path, "/")
          branch = repository[:branch] || "master"

          [
            %Repository{
              service_name: service_name,
              uri: location,
              branch: branch
            }
          ]
        else
          _ -> []
        end

      _ ->
        []
    end)
    |> Enum.each(&start_git/1)
  end

  @spec start_git(repository :: GitProvider.Git.Repository.t()) ::
          Supervisor.on_start_child()
  def start_git(repository) do
    name =
      repository.uri
      |> URI.parse()
      |> Map.get(:path)
      |> String.trim_leading("/")

    repository = %Repository{
      repository
      | name: name,
        service_name: repository.service_name || name
    }

    GitProvider.Git.Server.start_child(repository)
  end
end
