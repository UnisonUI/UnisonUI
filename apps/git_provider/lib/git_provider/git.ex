defmodule GitProvider.Git do
  use GenStage
  require Logger
  alias GitProvider.Git.Repository
  alias Common.Events.{Down, Up}
  alias Common.Service.{Grpc, OpenApi, Metadata}
  alias GitProvider.Git.Configuration

  @git_cmd "git"
  defp pull_interval, do: Durex.ms!(Application.fetch_env!(:git_provider, :pull_interval))

  @typep state :: {Repository.t(), binary()}

  @spec start_link(repository :: Repository.t()) :: GenServer.on_start() | {:error, :bad_arg}
  def start_link(%Repository{} = repository),
    do: GenStage.start_link(__MODULE__, repository, name: String.to_atom(repository.name))

  def start_link(_repository), do: {:error, :bad_arg}

  @spec init(repository :: Repository.t()) :: {:producer, state()}
  @impl true
  def init(repository) do
    repository = Repository.create_temp_dir(repository)
    send(self(), :pull)
    {:producer, {repository, ""}}
  end

  @impl true
  def handle_demand(_demand, state), do: {:noreply, [], state}

  @impl true
  def terminate(_, {%Repository{directory: directory}, _sha1}), do: File.rm(directory)

  defp schedule_pull, do: Process.send_after(self(), :pull, pull_interval())

  defp clone_or_pull({%Repository{uri: uri, branch: branch, directory: directory}, ""}),
    do:
      @git_cmd
      |> System.cmd(
        ["clone", "--branch", branch, "--single-branch", "--depth", "1", uri, directory],
        stderr_to_stdout: true
      )
      |> handle_system_cmd

  defp clone_or_pull({%Repository{directory: directory}, _}),
    do:
      @git_cmd
      |> System.cmd(["pull"], cd: directory, stderr_to_stdout: true)
      |> handle_system_cmd

  defp get_latest_sha1(%Repository{directory: directory, branch: branch}),
    do:
      @git_cmd
      |> System.cmd(["rev-parse", "--verify", branch], cd: directory, stderr_to_stdout: true)
      |> handle_system_cmd

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

  defp retrieve_files({%Repository{directory: directory}, sha1}) do
    with {:ok, changed_files} <-
           System.cmd(@git_cmd, ["diff", "--name-only", sha1, "HEAD"],
             cd: directory,
             stderr_to_stdout: true
           )
           |> handle_system_cmd do
      {:ok,
       String.split(changed_files, "\n", trim: true)
       |> Enum.map(fn file -> directory |> Path.join(file) |> Path.expand() end)}
    end
  end

  defp handle_system_cmd({result, 0}), do: {:ok, result}
  defp handle_system_cmd({error, _}), do: {:error, error}

  @impl true
  def handle_info(:pull, {%Repository{directory: directory} = repository, _} = state) do
    with {:ok, _} <- clone_or_pull(state),
         {:ok, raw_latest_sha1} <- get_latest_sha1(repository),
         latest_sha1 <- String.trim(raw_latest_sha1),
         {:ok, files} <- retrieve_files(state),
         configuration_file <- find_config_file(directory),
         {new_repository, events} <-
           find_specifications_files(repository, configuration_file, files) do
      events =
        events
        |> Stream.flat_map(&load_file/1)
        |> Stream.map(&to_event(&1, repository))
        |> Enum.to_list()

      schedule_pull()
      {:noreply, events, {new_repository, latest_sha1}}
    else
      {:error, reason} ->
        Logger.warn(reason, provider: "git_provider")
        schedule_pull()
        {:noreply, [], state}
    end
  end

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

  defp load_file({:upsert, {:openapi, path, info}}) do
    case File.read(path) do
      {:error, reason} ->
        Logger.warn(inspect(reason))
        []

      {:ok, content} ->
        [{:upsert, {:openapi, content, info}}]
    end
  end

  defp load_file({:upsert, {:grpc, path, info}}) do
    case UGRPC.Protobuf.compile(path) do
      {:error, reason} ->
        Logger.warn(inspect(reason))
        []

      {:ok, schema} ->
        [{:upsert, {:grpc, path, schema, info}}]
    end
  end

  defp load_file(data), do: [data]

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
                 String.starts_with?(file, path)
               end) do
            nil -> []
            spec -> [{:upsert, spec}]
          end
        end)

      to_delete =
        current_specifications
        |> Enum.reject(fn {_, path, _} ->
          Enum.any?(new_specifications, &match?({_, ^path, _}, &1))
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
