defmodule GitProvider.Git do
  use Clustering.GlobalServer, supervisor: GitProvider.Git.DynamicSupervisor
  require Logger

  alias GitProvider.Git.Repository
  alias Common.Events.{Down, Up}
  alias Common.Service.{Grpc, OpenApi, Metadata}
  alias GitProvider.Git.Configuration
  alias GitProvider.Git.Command

  @typep state :: {Repository.t(), binary()}

  defp pull_interval, do: Durex.ms!(Application.fetch_env!(:git_provider, :pull_interval))
  defp services_behaviour, do: Application.fetch_env!(:services, :behaviour)

  @spec init(repository :: Repository.t()) :: {:ok, state(), {:continue, :wait_for_ra}}
  @impl true
  def init(repository) do
    Process.flag(:trap_exit, true)
    repository = Repository.create_temp_dir(repository)
    {:ok, {repository, ""}, {:continue, :wait_for_ra}}
  end

  @impl true
  def handle_continue(:wait_for_ra, state) do
    if Services.Cluster.running?() do
      send(self(), :pull)
      {:noreply, state}
    else
      Process.sleep(1_000)

      {:noreply, state, {:continue, :wait_for_ra}}
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
      |> Stream.flat_map(&read_content/1)
      |> Stream.map(&to_event(&1, repository))
      |> Enum.to_list()
      |> IO.inspect()
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

  @spec to_event(
          {:delete, String.t()}
          | {:upsert, {:openapi, String.t(), GitProvider.Git.Configuration.OpenApi.spec()}}
          | {:upsert,
             {:grpc, String.t(), UGRPC.Protobuf.Structs.Schema.t(),
              GitProvider.Git.Configuration.Grpc.spec()}},
          GitProvider.Git.Repository.t()
        ) :: Common.Events.t()

  defp to_event({:delete, path}, %Repository{name: name, directory: directory}) do
    path = String.replace_prefix(path, directory, "") |> String.trim_leading("/")
    %Down{id: "#{name}:#{path}"}
  end

  defp to_event({:upsert, {:openapi, content, kw}}, %Repository{
         name: name,
         uri: uri,
         directory: directory
       }) do
    provider = uri |> URI.parse() |> Map.get(:host) |> String.replace_prefix("www.", "")
    service_name = Keyword.get(kw, :name)
    use_proxy = Keyword.get(kw, :use_proxy)

    filename =
      kw
      |> Keyword.get(:path)
      |> String.replace_prefix(directory, "")
      |> String.trim_leading("/")

    id = "#{name}:#{filename}"

    service = %OpenApi{
      id: id,
      name: service_name,
      content: content,
      use_proxy: use_proxy,
      metadata: %Metadata{provider: provider, file: filename}
    }

    %Up{service: service}
  end

  defp to_event({:upsert, {:grpc, path, schema, kw}}, %Repository{
         name: name,
         uri: uri,
         directory: directory
       }) do
    provider = uri |> URI.parse() |> Map.get(:host) |> String.replace_prefix("www.", "")
    service_name = Keyword.get(kw, :name)
    servers = Keyword.get(kw, :servers)

    filename =
      path
      |> String.replace_prefix(directory, "")
      |> String.trim_leading("/")

    id = "#{name}:#{filename}"

    service = %Grpc{
      id: id,
      name: service_name,
      schema: schema,
      servers: servers,
      metadata: %Metadata{provider: provider, file: filename}
    }

    %Up{service: service}
  end

  defp read_content({:upsert, {:openapi, path, info}}) do
    case File.read(path) do
      {:error, reason} ->
        Logger.warn(inspect(reason))
        []

      {:ok, content} ->
        [{:upsert, {:openapi, content, info}}]
    end
  end

  defp read_content({:upsert, {:grpc, path, info}}) do
    case UGRPC.Protobuf.compile(path) do
      {:error, reason} ->
        Logger.warn(inspect(reason))
        []

      {:ok, schema} ->
        [{:upsert, {:grpc, path, schema, info}}]
    end
  end

  defp read_content(data), do: [data]

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
           read_config(configuration_file) do
      openapi = extract_specifications(openapi, directory, service_name, repo_service_name)
      grpc = extract_specifications(grpc, directory, service_name, repo_service_name)

      new_specifications = openapi ++ grpc

      changed_files =
        detect_changes(
          files,
          configuration_file,
          directory,
          current_specifications,
          new_specifications
        )

      to_add =
        Enum.flat_map(changed_files, fn file ->
          case Enum.find(new_specifications, fn {_, path, _} ->
                 String.starts_with?(file, path) && File.exists?(file)
               end) do
            nil -> []
            spec -> [{:upsert, spec}]
          end
        end)

      to_delete =
        current_specifications
        |> Enum.reject(fn {_, path, _} ->
          Enum.any?(new_specifications, &match?({_, ^path, _}, &1)) && File.exists?(path)
        end)
        |> Enum.map(&{:delete, elem(&1, 1)})

      new_repository = %Repository{repository | specifications: new_specifications}
      {new_repository, to_delete ++ to_add}
    else
      _ -> {repository, []}
    end
  end

  defp extract_specifications(
         %{use_proxy: use_proxy, specifications: specifications},
         directory,
         service_name,
         repo_service_name
       ),
       do:
         specifications
         |> Enum.map(fn kw ->
           {path, kw} =
             Keyword.get_and_update!(kw, :path, fn path ->
               path = directory |> Path.join(path) |> Path.expand()
               {path, path}
             end)

           kw =
             Keyword.update!(kw, :name, fn
               nil -> service_name || repo_service_name
               name -> name
             end)
             |> Keyword.update!(:use_proxy, fn
               nil -> use_proxy
               proxy -> proxy
             end)

           {:openapi, path, kw}
         end)

  defp extract_specifications(
         %{servers: servers, files: files},
         directory,
         service_name,
         repo_service_name
       ),
       do:
         files
         |> Enum.map(fn {path, kw} ->
           path = directory |> Path.join(path) |> Path.expand()

           kw =
             kw
             |> Keyword.update!(:name, fn
               nil -> service_name || repo_service_name
               name -> name
             end)
             |> Keyword.update!(:servers, fn
               [] -> servers
               servers -> servers
             end)

           {:grpc, path, kw}
         end)

  defp extract_specifications(_, _directory, _service_name, _repo_service_name), do: []
  defp read_config(configuration_file), do: Configuration.from_file(configuration_file)

  defp detect_changes(
         files,
         configuration_file,
         directory,
         current_specifications,
         new_specifications
       ) do
    if Enum.any?(files, &(&1 == configuration_file)) do
      unchanged =
        current_specifications
        |> Enum.filter(fn {_, path, _} ->
          Enum.any?(new_specifications, fn {_, new_path, _} -> path == new_path end)
        end)

      directory
      |> list_files()
      |> Enum.reject(fn file ->
        Enum.any?(unchanged, fn {_, path, _} ->
          path = directory |> Path.join(path) |> Path.expand()
          String.starts_with?(file, path)
        end)
      end)
    else
      files
    end
  end
end
