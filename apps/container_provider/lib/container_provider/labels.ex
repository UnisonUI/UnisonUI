defmodule ContainerProvider.Labels do
  @type t :: %__MODULE__{
          service_name: String.t(),
          openapi: [
            port: pos_integer(),
            specification_path: String.t(),
            protocol: String.t(),
            use_proxy: boolean()
          ],
          grpc: [port: pos_integer(), tls: boolean()]
        }
  @type openapi :: [endpoint: String.t(), use_proxy: boolean()]
  @type grpc :: [address: String.t(), port: pos_integer(), use_tls: boolean()]
  defstruct [:service_name, :openapi, :grpc]

  def from_map(labels) when is_map(labels) do
    config_labels = labels()
    service_name = labels[config_labels[:service_name]] || labels["name"]

    openapi =
      with openapi when is_list(openapi) <- config_labels[:openapi],
           port when not is_nil(port) <- labels[openapi[:port]],
           {port, _} <- Integer.parse(port),
           specification_path <- labels[openapi[:specification_path]] || "/specification.yaml",
           protocol <- labels[openapi[:protocol]] || "http",
           use_proxy <- labels[openapi[:use_proxy]] || "false" do
        [
          port: port,
          specification_path: specification_path,
          protocol: protocol,
          use_proxy: use_proxy == "true"
        ]
      else
        _ -> nil
      end

    grpc =
      with grpc when is_list(grpc) <- config_labels[:grpc],
           port when not is_nil(port) <- labels[grpc[:port]],
           {port, _} <- Integer.parse(port),
           tls <- labels[grpc[:tls]] || "false" do
        [port: port, tls: tls == "true"]
      else
        _ -> nil
      end

    %__MODULE__{service_name: service_name, openapi: openapi, grpc: grpc}
  end

  @spec extract_endpoint(labels :: t(), ip :: String.t() | nil) ::
          [service_name: String.t(), openapi: openapi(), grpc: grpc()] | nil
  def extract_endpoint(_, nil), do: nil

  def extract_endpoint(%__MODULE__{service_name: service_name, openapi: openapi, grpc: grpc}, ip) do
    openapi =
      if is_nil(openapi) do
        nil
      else
        endpoint =
          "#{openapi[:protocol]}://#{ip}:#{openapi[:port]}#{openapi[:specification_path]}"

        [endpoint: endpoint, use_proxy: openapi[:use_proxy]]
      end

    grpc =
      if is_nil(grpc) do
        nil
      else
        [address: ip, port: grpc[:port], use_tls: grpc[:tls]]
      end

    [service_name: service_name, openapi: openapi, grpc: grpc]
  end

  defp labels, do: Application.fetch_env!(:container_provider, :labels)
end
