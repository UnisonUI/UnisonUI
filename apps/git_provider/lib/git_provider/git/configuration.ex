defmodule GitProvider.Git.Configuration do
  import Norm
  use OK.Pipe

  alias GitProvider.Git.Configuration.{OpenApi, Grpc}

  @type t :: %__MODULE__{
          version: String.t(),
          name: String.t() | nil,
          openapi: OpenApi.t() | nil,
          grpc: Grpc.t() | nil
        }
  defstruct [:name, :openapi, :grpc, version: "1"]

  def s,
    do:
      one_of([
        schema(%__MODULE__{
          version: "1",
          name: spec(is_binary() or is_nil()),
          openapi: one_of([spec(is_nil()), OpenApi.s()]),
          grpc: spec(is_nil())
        }),
        schema(%__MODULE__{
          version: spec(is_binary() and (&(&1 != "1"))),
          name: spec(is_binary() or is_nil()),
          openapi: one_of([spec(is_nil()), OpenApi.s()]),
          grpc: one_of([spec(is_nil()), Grpc.s()])
        })
      ])

  @spec from_file(path :: String.t()) :: {:ok, t()} | {:error, term()}
  def from_file(path) do
    YamlElixir.read_from_file(path) ~> decode() ~>> conform(s())
  end

  defp decode(keywords) do
    fields =
      keywords
      |> Enum.map(fn
        {"openapi", value} when is_map(value) ->
          {:openapi, OpenApi.decode(value)}

        {"specifications", value} when is_list(value) ->
          {:openapi, OpenApi.decode(%{"specifications" => value})}

        {"grpc", value} when is_map(value) ->
          {:grpc, Grpc.decode(value)}

        {"useProxy", value} ->
          {:use_proxy, value}

        {key, value} ->
          {String.to_atom(key), value}
      end)
      |> Enum.into(%{})

    new_fields =
      case {Map.get(fields, :use_proxy), Map.get(fields, :version, "1")} do
        {use_proxy, "1"} when is_boolean(use_proxy) ->
          fields
          |> Map.update(:openapi, %{}, fn openapi ->
            %{openapi | use_proxy: use_proxy}
          end)
          |> Map.delete(:use_proxy)

        _ ->
          fields
      end

    struct!(__MODULE__, new_fields)
  end

  defmodule OpenApi do
    @type spec :: [path: String.t(), name: String.t() | nil, use_proxy: boolean() | nil]
    @type t :: %__MODULE__{
            specifications: [spec()],
            use_proxy: boolean()
          }
    defstruct [:specifications, use_proxy: false]
    def s, do: schema(%__MODULE__{use_proxy: spec(is_boolean())})

    def decode(keywords) do
      fields =
        keywords
        |> Enum.flat_map(fn
          {"specifications", value} when is_list(value) ->
            new_spec =
              value
              |> Enum.map(fn
                path when is_binary(path) ->
                  [path: path, name: nil, use_proxy: false]

                spec when is_map(spec) ->
                  spec
                  |> Enum.reduce([path: nil, name: nil, use_proxy: false], fn
                    {"path", path}, acc ->
                      Keyword.update!(acc, :path, fn _ -> to_string(path) end)

                    {"name", name}, acc ->
                      Keyword.update!(acc, :name, fn _ -> to_string(name) end)

                    {"useProxy", use_proxy}, acc ->
                      Keyword.update!(acc, :use_proxy, fn _ -> use_proxy end)

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

      struct!(__MODULE__, fields)
    end
  end

  defmodule Grpc do
    @type servers :: %{String.t() => Common.Service.Grpc.server()}
    @type spec :: [name: String.t() | nil, servers: servers()]
    @type t :: %__MODULE__{
            servers: servers(),
            files: %{String.t() => spec()}
          }
    defstruct [:servers, :files]

    def s, do: schema(%__MODULE__{})

    defp decode_server(keywords) do
      server =
        Enum.reduce(keywords, [address: nil, port: nil, use_tls: false, name: nil], fn
          {"address", address}, acc ->
            Keyword.update!(acc, :address, fn _ -> to_string(address) end)

          {"name", name}, acc ->
            Keyword.update!(acc, :name, fn _ -> to_string(name) end)

          {"port", port}, acc ->
            Keyword.update!(acc, :port, fn _ -> port end)

          {"useTls", use_tls}, acc ->
            Keyword.update!(acc, :use_tls, fn _ -> use_tls end)

          _, acc ->
            acc
        end)

      {server[:name] || "#{server[:address]}:#{server[:port]}", Keyword.delete(server, :name)}
    end

    defp decode_file({file, keywords}),
      do:
        {file,
         Enum.reduce(keywords, [name: nil, servers: %{}], fn
           {"name", name}, acc ->
             Keyword.update!(acc, :name, fn _ -> to_string(name) end)

           {"servers", servers}, acc ->
             Keyword.update!(acc, :servers, fn _ ->
               servers |> Enum.map(&decode_server/1) |> Enum.into(%{})
             end)

           _, acc ->
             acc
         end)}

    def decode(keywords) do
      fields =
        keywords
        |> Enum.map(fn
          {"servers", value} when is_list(value) ->
            {:servers, value |> Enum.map(&decode_server/1) |> Enum.into(%{})}

          {"protobufs", value} when is_map(value) ->
            {:files, Enum.map(value, &decode_file/1) |> Enum.into(%{})}
        end)

      struct!(__MODULE__, fields)
    end
  end
end
