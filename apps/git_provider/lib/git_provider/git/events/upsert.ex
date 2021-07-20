defmodule GitProvider.Git.Events.Upsert do
  @type t ::
          GitProvider.Git.Events.Upsert.OpenApi.t()
          | GitProvider.Git.Events.Upsert.Grpc.t()
  defmodule OpenApi do
    @type t :: %__MODULE__{
            path: String.t(),
            content: String.t(),
            specs: GitProvider.Git.Configuration.OpenApi.spec(),
            repository: GitProvider.Git.Repository.t()
          }
    defstruct [:path, :content, :specs, :repository]

    defimpl Common.Events.Converter, for: __MODULE__ do
      alias GitProvider.Git.Events.Upsert
      alias GitProvider.Git.Repository
      alias Common.Events.Up
      alias Common.Service.{OpenApi, Metadata}

      def to_event(%Upsert.OpenApi{
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

        service = %OpenApi{
          id: id,
          name: service_name,
          content: content,
          use_proxy: use_proxy,
          metadata: %Metadata{provider: provider, file: filename}
        }

        %Up{service: service}
      end
    end

    defimpl GitProvider.Git.Events.ContentLoader, for: __MODULE__ do
      alias GitProvider.Git.Events.Upsert
      require OK

      def load_content(%Upsert.OpenApi{path: path} = event) do
        OK.for do
          content <- File.read(path)
        after
          %Upsert.OpenApi{event | content: content}
        end
      end
    end
  end

  defmodule Grpc do
    @type t :: %__MODULE__{
            path: String.t(),
            schema: UGRPC.Protobuf.Structs.Schema.t(),
            specs: GitProvider.Git.Configuration.Grpc.spec(),
            repository: GitProvider.Git.Repository.t()
          }
    defstruct [:path, :schema, :specs, :repository]

    defimpl Common.Events.Converter, for: __MODULE__ do
      alias GitProvider.Git.Events.Upsert
      alias GitProvider.Git.Repository
      alias Common.Events.Up
      alias Common.Service.{Grpc, Metadata}

      def to_event(%Upsert.Grpc{
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
          schema <- UGRPC.Protobuf.compile(path)
        after
          %Upsert.Grpc{event | schema: schema}
        end
      end
    end
  end
end
