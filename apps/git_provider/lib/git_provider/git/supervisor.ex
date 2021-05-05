defmodule GitProvider.Git.Supervisor do
  @dialyzer :no_match
  use DynamicSupervisor
  alias GitProvider.Git.Repository

  def start_link, do: DynamicSupervisor.start_link(__MODULE__, nil, name: __MODULE__)

  def init(_) do
    DynamicSupervisor.init(strategy: :one_for_one)
  end

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
          DynamicSupervisor.on_start_child()
  def start_git(repository) do
    repository = %Repository{
      repository
      | name:
          repository.uri
          |> URI.parse()
          |> Map.get(:path)
          |> String.trim_leading("/")
    }

    with true <- provider_enabled(),
         {:ok, pid} <-
           DynamicSupervisor.start_child(__MODULE__, %{
             id: repository,
             start: {GitProvider.Git, :start_link, [repository]},
             restart: :temporary
           }) do
      _ = services_behaviour().add_source(pid)
      {:ok, pid}
    else
      false -> :ignore
      error -> error
    end
  end

  defp provider_enabled, do: Application.fetch_env!(:git_provider, :enabled)
  defp services_behaviour, do: Application.fetch_env!(:services, :behaviour)

  @spec stop_git(pid()) :: :ok | {:error, :not_found}
  def stop_git(pid), do: DynamicSupervisor.terminate_child(__MODULE__, pid)
end
