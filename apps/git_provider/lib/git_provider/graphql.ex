defmodule GitProvider.GraphQL do
  use Clustering.GlobalServer
  require Logger
  alias GitProvider.GraphQL.{Repositories, Settings}
  alias GitProvider.Git.{Repository, Supervisor}

  @typep state ::
           {module(), GitProvider.GraphQL.Settings.t(), GitProvider.GraphQL.Repositories.t()}

  @impl true
  @spec init({module(), settings :: GitProvider.GraphQL.Settings.t()}) :: {:ok, state()}
  def init({client, settings}) do
    send(self(), :retrieve_projects)

    patterns =
      settings.patterns
      |> Stream.filter(&is_binary/1)
      |> Stream.map(&Regex.compile/1)
      |> Stream.filter(&match?({:ok, _}, &1))
      |> Enum.into([], &elem(&1, 1))

    {:ok, {client, %Settings{settings | patterns: patterns}, Repositories.new()}}
  end

  @impl true
  def handle_info(
        :retrieve_projects,
        {client,
         %Settings{
           api_uri: api_uri,
           api_token: token,
           polling_interval: polling_interval,
           patterns: patterns
         } = settings, current_repositories} = state
      ) do
    new_state =
      with {:ok, projects} <- client.list_projects(api_uri, token) do
        matched_new_repositories =
          Repositories.match_new_repositories(
            current_repositories,
            projects,
            patterns,
            token
          )
          |> Stream.map(fn %Repository{service_name: name} = repository ->
            Logger.debug("Matching repo #{name}", provider: "git_provider")
            start_git(repository)
          end)
          |> Enum.reject(&is_nil/1)

        new_repositories = Repositories.update(current_repositories, matched_new_repositories)

        schedule_polling(polling_interval)
        {client, settings, new_repositories}
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
