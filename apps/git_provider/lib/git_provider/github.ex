defmodule GitProvider.Github do
  # @dialyzer :no_match
  use GenServer
  require Logger
  alias GitProvider.Github.{Client, Node, Settings}
  alias GitProvider.Git.{Repository, Supervisor}

  @typep state :: {GitProvider.Github.Settings.t(), [{String.t(), pid}]}

  @spec start_link(settings :: GitProvider.Github.Settings.t()) :: GenServer.on_start()
  def start_link(settings), do: GenServer.start_link(__MODULE__, settings, name: __MODULE__)

  @impl true
  @spec init(settings :: GitProvider.Github.Settings.t()) :: {:ok, state()}
  def init(settings) do
    send(__MODULE__, :retrieve_projects)
    {:ok, {settings, []}}
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
    repositories =
      repositories
      |> Stream.map(&Regex.compile/1)
      |> Stream.filter(&match?({:ok, _}, &1))
      |> Stream.map(&elem(&1, 1))
      |> Enum.to_list()

    new_state =
      with {:ok, github_repositories} <- Client.list_projects(api_uri, token) do
        new_repositories =
          github_repositories
          |> Stream.filter(fn %Node{name: name} ->
            Enum.any?(repositories, &Regex.match?(&1, name))
          end)
          |> Stream.reject(fn %Node{name: name} ->
            Enum.any?(current_repositories, fn {current, _} -> current == name end)
          end)
          |> Stream.map(fn %Node{name: name, url: url, branch: branch} ->
            url_with_authority =
              url |> URI.parse() |> Map.update!(:userinfo, fn _ -> token end) |> URI.to_string()

            Logger.debug("Matching repo #{name}", provider: "git_provider")
            %Repository{service_name: name, uri: url_with_authority, branch: branch}
          end)
          |> Stream.map(&start_git/1)
          |> Stream.filter(&match?({:ok, _}, &1))
          |> Stream.map(&elem(&1, 1))
          |> Enum.reduce(current_repositories, fn repository, repositories ->
            [repository | repositories]
          end)

        schedule_polling(polling_interval)
        {settings, new_repositories}
      else
        {:error, message} ->
          Logger.warn("Error with github: #{message}")
          state
      end

    {:noreply, new_state}
  end

  @impl true
  def handle_info({:DOWN, _reference, :process, pid, _type}, {settings, repositories}) do
    new_repositories = Enum.reject(repositories, &match?({_, ^pid}, &1))
    {:noreply, {settings, new_repositories}}
  end

  defp start_git(%Repository{service_name: name} = repository) do
    with {:ok, pid} <- Supervisor.start_git(repository) do
      Process.monitor(pid)
      {:ok, {name, pid}}
    else
      error -> error
    end
  end

  @impl true
  def terminate(_reason, {_settings, repositories}) do
    Enum.each(repositories, fn {_, pid} -> Supervisor.stop_git(pid) end)
  end

  defp schedule_polling(interval),
    do: Process.send_after(__MODULE__, :retrieve_projects, interval)
end
