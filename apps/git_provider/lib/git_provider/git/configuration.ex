defmodule GitProvider.Git.Configuration do
  use OK.Pipe
  use TypeCheck

  alias GitProvider.Git.Configuration.{AsyncOpenApi, Grpc}

  defstruct [:name, :openapi, :asyncapi, :grpc, :version]

  @type! t :: %__MODULE__{
           version: literal("2"),
           name: String.t() | nil,
           openapi: AsyncOpenApi.t() | nil,
           asyncapi: AsyncOpenApi.t() | nil,
           grpc: Grpc.t() | nil
         }

  @spec from_file(path :: String.t()) :: {:ok, t()} | {:error, term()}
  def from_file(path) do
    YamlElixir.read_from_file(path) ~> decode() ~>> TypeCheck.dynamic_conforms(t())
  end

  defp decode(keywords) do
    fields =
      Enum.map(keywords, fn
        {"asyncapi", value} when is_map(value) ->
          {:asyncapi, AsyncOpenApi.decode(value)}

        {"openapi", value} when is_map(value) ->
          {:openapi, AsyncOpenApi.decode(value)}

        {"specifications", value} when is_list(value) ->
          {:openapi, AsyncOpenApi.decode(%{"specifications" => value})}

        {"grpc", value} when is_map(value) ->
          {:grpc, Grpc.decode(value)}

        {key, value} ->
          {String.to_atom(key), value}
      end)

    struct(__MODULE__, fields)
  end

  defmodule AsyncOpenApi do
    defmodule Specification do
      use TypeCheck
      defstruct [:path, :name, use_proxy: false]

      @type! t :: %__MODULE__{
               path: String.t(),
               name: String.t() | nil,
               use_proxy: boolean()
             }
    end

    use TypeCheck
    alias __MODULE__.Specification

    defstruct [:specifications, use_proxy: false]

    @type! t :: %__MODULE__{
             specifications: [Specification.t()],
             use_proxy: boolean()
           }

    def decode(keywords) do
      fields =
        keywords
        |> Enum.flat_map(fn
          {"specifications", value} when is_list(value) ->
            new_spec =
              value
              |> Enum.map(fn
                path when is_binary(path) ->
                  %Specification{path: path}

                spec when is_map(spec) ->
                  spec
                  |> Enum.reduce(%Specification{}, fn
                    {"path", path}, acc ->
                      %Specification{acc | path: to_string(path)}

                    {"name", name}, acc ->
                      %Specification{acc | name: to_string(name)}

                    {"useProxy", use_proxy}, acc ->
                      %Specification{acc | use_proxy: to_string(use_proxy)}

                    _, acc ->
                      acc
                  end)
              end)

            [{:specifications, new_spec}]

          {"useProxy", value} ->
            [{:use_proxy, value}]

          _ ->
            []
        end)

      struct(__MODULE__, fields)
    end
  end

  defmodule Grpc do
    use TypeCheck

    # %{binary() => Services.Service.Grpc.Server.t()}
    @opaque! servers :: map()

    defmodule Specification do
      use TypeCheck
      defstruct [:name, :servers]

      @type! t :: %__MODULE__{
               name: String.t() | nil,
               servers: GitProvider.Git.Configuration.Grpc.servers()
             }
    end

    alias __MODULE__.Specification
    alias Services.Service.Grpc.Server

    defstruct [:files, servers: %{}]

    @type! t :: %__MODULE__{
             servers: servers(),
             # (%{binary() => Specification.t()}
             files: map()
           }

    defp decode_server(keywords) do
      {server, name} =
        Enum.reduce(keywords, {%Server{}, nil}, fn
          {"address", address}, {acc, name} ->
            {%Server{acc | address: to_string(address)}, name}

          {"name", name}, {acc, _} ->
            {acc, name}

          {"port", port}, {acc, name} ->
            {%Server{acc | port: port}, name}

          {"useTls", use_tls}, {acc, name} ->
            {%Server{acc | use_tls: use_tls}, name}

          _, acc ->
            acc
        end)

      {name || "#{server.address}:#{server.port}", server}
    end

    defp decode_file({file, keywords}),
      do:
        {file,
         Enum.reduce(keywords, %Specification{}, fn
           {"name", name}, acc ->
             %Specification{acc | name: to_string(name)}

           {"servers", servers}, acc ->
             %Specification{acc | servers: Enum.into(servers, %{}, &decode_server/1)}

           _, acc ->
             acc
         end)}

    def decode(keywords) do
      fields =
        keywords
        |> Enum.map(fn
          {"servers", value} when is_list(value) ->
            {:servers, Enum.into(value, %{}, &decode_server/1)}

          {"protobufs", value} when is_map(value) ->
            {:files, Enum.into(value, %{}, &decode_file/1)}
        end)

      struct(__MODULE__, fields)
    end
  end
end
