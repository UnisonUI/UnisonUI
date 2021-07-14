defmodule GitProvider.Git.Server do
  use Clustering.GlobalServer, supervisor: GitProvider.Git.DynamicSupervisor
  require Logger

  alias GitProvider.Git.{Event, Events, Command, Configuration, Repository, Specifications}

  @typep state :: {Repository.t(), binary()}

  defp pull_interval, do: Durex.ms!(Application.fetch_env!(:git_provider, :pull_interval))
  defp services_behaviour, do: Application.fetch_env!(:services, :storage_backend)

  @spec init(repository :: Repository.t()) :: {:ok, state(), {:continue, :wait_for_storage}}
  @impl true
  def init(repository) do
    Process.flag(:trap_exit, true)
    repository = Repository.create_temp_dir(repository)
    {:ok, {repository, ""}, {:continue, :wait_for_storage}}
  end

  @impl true
  def handle_continue(:wait_for_storage, state) do
    if services_behaviour().alive?() do
      send(self(), :pull)
      {:noreply, state}
    else
      Process.sleep(1_000)

      {:noreply, state, {:continue, :wait_for_storage}}
    end
  end

  def child_spec(%Repository{name: name} = repository),
    do: %{
      id: "Git_#{name}",
      start: {__MODULE__, :start_link, [[data: repository, name: String.to_atom(name)]]},
      restart: :transient
    }

  @impl true
  def terminate(_, {%Repository{directory: directory}, _sha1}) do
    File.rm_rf(directory)
  end

  defp schedule_pull, do: Process.send_after(self(), :pull, pull_interval())

  defp clone_or_pull({%Repository{uri: uri, branch: branch, directory: directory}, ""}),
    do: Command.clone(uri, branch, directory)

  defp clone_or_pull({%Repository{directory: directory}, _}),
    do: Command.pull(directory)

  defp get_latest_hash(%Repository{directory: directory, branch: branch}),
    do: Command.hash(branch, directory)

  defp list_files(directory),
    do:
      directory
      |> Path.join("**")
      |> Path.wildcard(match_dot: true)
      |> Stream.map(&{File.stat(&1), &1})
      |> Stream.filter(&match?({{:ok, %File.Stat{type: type}}, _} when type != :directory, &1))
      |> Stream.map(&elem(&1, 1))
      |> Enum.to_list()

  defp retrieve_files({%Repository{directory: directory}, ""}),
    do: {:ok, list_files(directory)}

  defp retrieve_files({%Repository{directory: directory}, hash}),
    do: Command.changes(hash, directory)

  @impl true
  def handle_info(:pull, {%Repository{directory: directory} = repository, _} = state) do
    with :ok <- clone_or_pull(state),
         {:ok, hash} <- get_latest_hash(repository),
         {:ok, files} <- retrieve_files(state),
         configuration_file <- find_config_file(directory),
         {new_repository, events} <-
           find_specifications_files(repository, configuration_file, files) do
      events
      |> Stream.flat_map(fn event ->
        case Event.load_content(event) do
          {:ok, event} ->
            [event]

          {:error, error} ->
            message =
              if Exception.exception?(error), do: Exception.message(error), else: inspect(error)

            Logger.warn(message)
            []
        end
      end)
      |> Stream.map(&Event.to_event(&1, repository))
      |> Enum.to_list()
      |> then(&services_behaviour().dispatch_events(&1))

      schedule_pull()
      {:noreply, {new_repository, hash}}
    else
      {:error, reason} ->
        Logger.warn(reason, provider: "git_provider")
        schedule_pull()
        {:noreply, state}
    end
  end

  def handle_info(_, state), do: {:noreply, state}

  defp find_config_file(directory) do
    restui = Path.join(directory, ".restui.yaml")
    unisonui = Path.join(directory, ".unisonui.yaml")

    if File.exists?(unisonui) do
      unisonui
    else
      if File.exists?(restui) do
        restui
      else
        nil
      end
    end
  end

  defp find_specifications_files(
         %Repository{
           directory: directory,
           service_name: repo_service_name,
           specifications: current_specifications
         } = repository,
         configuration_file,
         files
       ) do
    with {:ok, %Configuration{name: service_name, openapi: openapi, grpc: grpc}} <-
           Configuration.from_file(configuration_file) do
      openapi =
        Specifications.from_configuration(openapi, directory, service_name, repo_service_name)

      grpc = Specifications.from_configuration(grpc, directory, service_name, repo_service_name)

      new_specifications = Specifications.merge(openapi, grpc)

      files_changed =
        detect_changes(
          files,
          configuration_file,
          directory,
          current_specifications,
          new_specifications
        )

      to_add =
        Specifications.new_files(new_specifications, files_changed)
        |> Events.from_specifications()

      added_removed_files =
        Enum.reduce(files_changed, to_add, fn file, events ->
          removed? = new_specifications.specifications[file] && !File.exists?(file)
          if removed?, do: [%Events.Delete{path: file} | events], else: events
        end)

      events =
        Specifications.deleted_files(current_specifications, new_specifications).specifications
        |> Enum.reduce(added_removed_files, fn {path, _}, events ->
          [%Events.Delete{path: path} | events]
        end)

      new_repository = %Repository{repository | specifications: new_specifications}
      {new_repository, events}
    else
      _ -> {repository, []}
    end
  end

  defp detect_changes(
         files,
         configuration_file,
         directory,
         current_specifications,
         new_specifications
       ) do
    if Enum.any?(files, &(&1 == configuration_file)) do
      %Specifications{specifications: unchanged} =
        Specifications.intersection(current_specifications, new_specifications)

      directory
      |> list_files()
      |> Enum.reject(fn file ->
        Enum.any?(unchanged, fn {path, _} ->
          path = directory |> Path.join(path) |> Path.expand()
          String.starts_with?(file, path)
        end)
      end)
    else
      files
    end
  end
end
