defmodule GitProvider.Github do
  use Clustering.GlobalServer
  require Logger
  alias GitProvider.Github.{Client, Settings}
  alias GitProvider.Github.Repositories
  alias GitProvider.Git.{Repository, Supervisor}

  @typep state :: {GitProvider.Github.Settings.t(), GitProvider.Github.Repositories.t()}

  @impl true
  @spec init(settings :: GitProvider.Github.Settings.t()) :: {:ok, state()}
  def init(settings) do
    send(self(), :retrieve_projects)

    repositories =
      settings.repositories
      |> Stream.filter(&is_binary/1)
      |> Stream.map(&Regex.compile/1)
      |> Stream.filter(&match?({:ok, _}, &1))
      |> Enum.into([], &elem(&1, 1))

    {:ok, {%Settings{settings | repositories: repositories}, %Repositories{}}}
  end

  @impl true
  def handle_info(
        :retrieve_projects,
        {%Settings{
           api_uri: api_uri,
           api_token: token,
           polling_interval: polling_interval,
           repositories: repositories
         } = settings, current_repositories} = state
      ) do
    new_state =
      with {:ok, projects} <- Client.list_projects(api_uri, token) do
        matched_new_repositories =
          Repositories.matches_new_repositories(
            current_repositories,
            projects,
            repositories,
            token
          )
          |> Stream.map(fn %Repository{service_name: name} = repository ->
            Logger.debug("Matching repo #{name}", provider: "git_provider")
            start_git(repository)
          end)
          |> Enum.reject(&is_nil/1)

        new_repositories = Repositories.update(current_repositories, matched_new_repositories)

        schedule_polling(polling_interval)
        {settings, new_repositories}
      else
        {:error, message} ->
          Logger.warn("Error with github: #{message}")
          state
      end

    {:noreply, new_state}
  end

  defp start_git(%Repository{service_name: name} = repository) do
    case Supervisor.start_git(repository) do
      {:ok, _} ->
        name

      _ ->
        nil
    end
  end

  defp schedule_polling(interval),
    do: Process.send_after(self(), :retrieve_projects, interval)
end
