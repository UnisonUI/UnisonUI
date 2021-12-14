defmodule GitProvider.Git.Events.Upsert do
  @type t ::
          GitProvider.Git.Events.Upsert.AsyncOpenApi.t()
          | GitProvider.Git.Events.Upsert.Grpc.t()
  defmodule AsyncOpenApi do
    @type t :: %__MODULE__{
            type: :openapi | :asyncapi,
            path: String.t(),
            content: String.t(),
            specs: GitProvider.Git.Configuration.OpenApi.spec(),
            repository: GitProvider.Git.Repository.t()
          }
    defstruct [:type, :path, :content, :specs, :repository]

    defimpl Services.Event.From, for: __MODULE__ do
      alias GitProvider.Git.Events.Upsert
      alias GitProvider.Git.Repository
      alias Services.Event.Up
      alias Services.{AsyncApi, OpenApi, Metadata}

      def from(%Upsert.AsyncOpenApi{
            type: type,
            path: path,
            content: content,
            specs: specs,
            repository:
              %Repository{
                name: name,
                directory: directory
              } = repo
          }) do
        provider = Repository.provider(repo)
        service_name = specs[:name]
        use_proxy = specs[:use_proxy]

        filename =
          path
          |> String.replace_prefix(directory, "")
          |> String.trim_leading("/")

        id = "#{name}:#{filename}"

        service = %{
          id: id,
          name: service_name,
          content: content,
          use_proxy: use_proxy,
          metadata: %Metadata{provider: provider, file: filename}
        }

        service =
          case type do
            :asyncapi -> struct(AsyncApi, service)
            :openapi -> struct(OpenApi, service)
          end

        %Up{service: service}
      end
    end

    defimpl GitProvider.Git.Events.ContentLoader, for: __MODULE__ do
      alias GitProvider.Git.Events.Upsert
      require OK

      def load_content(%Upsert.AsyncOpenApi{path: path} = event) do
        OK.for do
          content <- File.read(path)
        after
          %Upsert.AsyncOpenApi{event | content: content}
        end
      end
    end
  end

  defmodule Grpc do
    @type t :: %__MODULE__{
            path: String.t(),
            schema: GRPC.Protobuf.Structs.Schema.t(),
            specs: GitProvider.Git.Configuration.Grpc.spec(),
            repository: GitProvider.Git.Repository.t()
          }
    defstruct [:path, :schema, :specs, :repository]

    defimpl Services.Event.From, for: __MODULE__ do
      alias GitProvider.Git.Events.Upsert
      alias GitProvider.Git.Repository
      alias Services.Event.Up
      alias Services.{Grpc, Metadata}

      def from(%Upsert.Grpc{
            path: path,
            schema: schema,
            specs: specs,
            repository:
              %Repository{
                name: name,
                directory: directory
              } = repo
          }) do
        provider = Repository.provider(repo)
        service_name = specs[:name]
        servers = specs[:servers]

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

    defimpl GitProvider.Git.Events.ContentLoader, for: __MODULE__ do
      alias GitProvider.Git.Events.Upsert
      require OK

      def load_content(%Upsert.Grpc{path: path} = event) do
        OK.for do
          schema <- GRPC.Protobuf.compile(path)
        after
          %Upsert.Grpc{event | schema: schema}
        end
      end
    end
  end
end
