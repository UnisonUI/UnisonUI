defmodule ContainerProvider.Labels do
  @type t :: %__MODULE__{
          service_name: String.t(),
          openapi: [
            port: pos_integer(),
            specification_path: String.t(),
            protocol: String.t(),
            use_proxy: boolean()
          ],
          asyncapi: [
            port: pos_integer(),
            specification_path: String.t(),
            protocol: String.t(),
            use_proxy: nil
          ],
          grpc: [port: pos_integer(), tls: boolean()]
        }
  @type async_openapi :: [endpoint: String.t(), use_proxy: boolean()]
  @type grpc :: [address: String.t(), port: pos_integer(), use_tls: boolean()]
  defstruct [:service_name, :openapi, :asyncapi, :grpc]

  def from_map(labels) when is_map(labels) do
    config_labels = labels()
    service_name = labels[config_labels[:service_name]] || labels["name"]

    openapi = extract_async_openapi_from_labels(config_labels[:openapi], labels)
    asyncapi = extract_async_openapi_from_labels(config_labels[:asyncapi], labels)

    grpc =
      with grpc when is_list(grpc) <- config_labels[:grpc],
           port when not is_nil(port) <- labels[grpc[:port]],
           {port, _} <- Integer.parse(port),
           tls <- labels[grpc[:tls]] || "false" do
        [port: port, tls: tls == "true"]
      else
        _ -> nil
      end

    %__MODULE__{service_name: service_name, openapi: openapi, asyncapi: asyncapi, grpc: grpc}
  end

  @spec valid?(labels :: t()) :: boolean()
  def valid?(%__MODULE__{openapi: openapi, asyncapi: asyncapi, grpc: grpc}),
    do: !is_nil(openapi) || !is_nil(asyncapi) || !is_nil(grpc)

  @spec extract_endpoint(labels :: t(), ip :: String.t() | nil) ::
          [
            service_name: String.t(),
            openapi: async_openapi() | nil,
            asyncapi: async_openapi() | nil,
            grpc: grpc() | nil
          ]
          | nil
  def extract_endpoint(_, nil), do: nil

  def extract_endpoint(
        %__MODULE__{service_name: service_name, openapi: openapi, asyncapi: asyncapi, grpc: grpc},
        ip
      ) do
    openapi = extract_async_openapi(openapi, ip)
    asyncapi = extract_async_openapi(asyncapi, ip)

    grpc =
      if is_nil(grpc) do
        nil
      else
        [address: ip, port: grpc[:port], use_tls: grpc[:tls]]
      end

    [service_name: service_name, openapi: openapi, asyncapi: asyncapi, grpc: grpc]
  end

  defp extract_async_openapi_from_labels(specification, labels) when is_list(specification) do
    with port when not is_nil(port) <- labels[specification[:port]],
         {port, _} <- Integer.parse(port),
         specification_path <-
           labels[specification[:specification_path]] || "/specification.yaml",
         protocol <- labels[specification[:protocol]] || "http" do
      use_proxy =
        case specification[:use_proxy] do
          nil -> nil
          specification -> (labels[specification] || "false") == "true"
        end

      [
        port: port,
        specification_path: specification_path,
        protocol: protocol,
        use_proxy: use_proxy
      ]
    else
      _ -> nil
    end
  end

  defp extract_async_openapi_from_labels(_, _), do: nil

  defp extract_async_openapi(nil, _), do: nil

  defp extract_async_openapi(source, ip) do
    endpoint = "#{source[:protocol]}://#{ip}:#{source[:port]}#{source[:specification_path]}"

    [endpoint: endpoint, use_proxy: source[:use_proxy]]
  end

  defp labels, do: Application.fetch_env!(:container_provider, :labels)
end
