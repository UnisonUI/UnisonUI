defprotocol GitProvider.Git.Events do
  @type t :: GitProvider.Git.Events.Delete.t() | GitProvider.Git.Events.Upsert.t()
  @spec to_event(t(), GitProvider.Git.Repository.t())::Common.Events.t()
  def to_event(event, repository)
  @spec path(t())::String.t()
  def path(event)

  @spec content(t()):: term()
  def content(event)

  defmodule Delete do
    @type t :: %__MODULE__{path: String.t()}
    defstruct [:path]
    defimpl GitProvider.Git.Events, for: __MODULE__ do

    end
  end

  defmodule Upsert do
    @type t :: %__MODULE__{path: String.t()}
    defstruct [:type, :path, :content]
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
end
