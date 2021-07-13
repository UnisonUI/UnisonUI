defmodule GitProvider.Git.Specifications do
  @type specification :: {
          :openapi | :grpc,
          GitProvider.Git.Configuration.OpenApi.spec()
          | GitProvider.Git.Configuration.Grpc.spec()
        }

  @type t :: %__MODULE__{specifications: %{String.t() => specification()}}
  defstruct specifications: %{}

  @spec from_configuration(
          config ::
            GitProvider.Git.Configuration.OpenApi.t() | GitProvider.Git.ConfigurationGrpc.t(),
          directory :: String.t(),
          service_name :: String.t(),
          repo_service_name :: String.t()
        ) :: t()
  def from_configuration(
        %{use_proxy: use_proxy, specifications: specifications},
        directory,
        service_name,
        repo_service_name
      ),
      do: %__MODULE__{
        specifications:
          Enum.into(specifications, %{}, fn specs ->
            path = directory |> Path.join(specs[:path]) |> Path.expand()

            specs =
              Keyword.update!(specs, :name, fn
                nil -> service_name || repo_service_name
                name -> name
              end)
              |> Keyword.update!(:use_proxy, fn
                nil -> use_proxy
                proxy -> proxy
              end)

            {path, {:openapi, specs}}
          end)
      }

  def from_configuration(
        %{servers: servers, files: files},
        directory,
        service_name,
        repo_service_name
      ),
      do: %__MODULE__{
        specifications:
          Enum.into(files, %{}, fn {path, specs} ->
            path = directory |> Path.join(path) |> Path.expand()

            specs =
              specs
              |> Keyword.update!(:name, fn
                nil -> service_name || repo_service_name
                name -> name
              end)
              |> Keyword.update!(:servers, fn
                [] -> servers
                servers -> servers
              end)

            {path, {:grpc, specs}}
          end)
      }

  def from_configuration(_, _directory, _service_name, _repo_service_name), do: %__MODULE__{}

  @spec new_files(specifications :: t(), files :: [String.t()]) :: t()
  def new_files(%__MODULE__{specifications: specifications}, files) do
    specifications =
      Enum.reduce(files, %{}, fn file, map ->
        case Enum.find(specifications, fn {path, _} -> String.starts_with?(file, path) end) do
          nil -> map
          {path, _} = specification -> Map.put(map, path, specification)
        end
      end)

    %__MODULE__{specifications: specifications}
  end

  @spec deleted_files(
          old_specifications :: t(),
          new_specifications :: t()
        ) :: t()
  def deleted_files(%__MODULE__{specifications: old_specifications}, %__MODULE__{
        specifications: new_specifications
      }) do
    from_specification =
      old_specifications
      |> Stream.reject(fn {path, _} -> !is_nil(new_specifications[path]) end)
      |> Enum.into(%{})

    specifications =
      Enum.reduce(new_specifications, from_specification, fn {path, _} = specification, map ->
        if File.exists?(path),
          do: Map.put_new(map, path, specification),
          else: map
      end)

    %__MODULE__{specifications: specifications}
  end

  @spec intersection(specs_1 :: t(), specs_2 :: t()) :: t()
  def intersection(%__MODULE__{specifications: specs_1}, %__MODULE__{specifications: specs_2}) do
    specifications =
      specs_1 |> Stream.filter(fn {path, _} -> !is_nil(specs_2[path]) end) |> Enum.into(%{})

    %__MODULE__{specifications: specifications}
  end

  @spec merge(specs_1 :: t(), specs_2 :: t()) :: t()
  def merge(%__MODULE__{specifications: specs_1}, %__MODULE__{specifications: specs_2}),
    do: %__MODULE__{specifications: Map.merge(specs_1, specs_2)}
end
