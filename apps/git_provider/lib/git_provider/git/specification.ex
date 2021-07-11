defmodule GitProvider.Git.Specification do
  @type t :: %__MODULE__{
          type: :openapi | :grpc,
          path: String.t(),
          specs:
            GitProvider.Git.Configuration.OpenApi.spec()
            | GitProvider.Git.Configuration.Grpc.spec()
        }
  defstruct [:type, :path, :specs]

  @spec from_configuration(
          config ::
            GitProvider.Git.Configuration.OpenApi.t() | GitProvider.Git.ConfigurationGrpc.t(),
          directory :: String.t(),
          service_name :: String.t(),
          repo_service_name :: String.t()
        ) :: [t()]
  def from_configuration(
         %{use_proxy: use_proxy, specifications: specifications},
         directory,
         service_name,
         repo_service_name
       ),
       do:
         specifications
         |> Enum.map(fn specs ->
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

           {:openapi, path, specs}
         end)

  def from_configuration(
         %{servers: servers, files: files},
         directory,
         service_name,
         repo_service_name
       ),
       do:
         files
         |> Enum.map(fn {path, specs} ->
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

           {:grpc, path, specs}
         end)

  def from_configuration(_, _directory, _service_name, _repo_service_name), do: []
end
