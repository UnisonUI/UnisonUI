defmodule ContainerProvider.Specifications do
  alias ContainerProvider.HttpClient
  alias Services.{Grpc, OpenApi, Metadata}
  require Logger
  def retrieve_specification(_, _, nil), do: nil

  def retrieve_specification(id, service_name, endpoint: endpoint, use_proxy: use_proxy) do
    with data when not is_nil(data) <- HttpClient.download_file(endpoint) do
      %URI{path: path} = URI.parse(endpoint)
      metadata = %Metadata{provider: "container", file: String.slice(path, 1..-1)}

      %OpenApi{
        id: id,
        name: service_name,
        content: data,
        use_proxy: use_proxy,
        metadata: metadata
      }
    end
  end

  def retrieve_specification(
        id,
        service_name,
        [address: ip, port: port, use_tls: use_tls] = server
      ) do
    protocol = if use_tls, do: "https", else: "http"
    address = "#{ip}:#{port}"
    endpoint = "#{protocol}://#{address}"

    with {:ok, schema} <- GRPC.Reflection.load_schema(endpoint) do
      metadata = %Metadata{provider: "container", file: address}

      %Grpc{
        id: id,
        name: service_name,
        schema: schema,
        servers: %{address => server},
        metadata: metadata
      }
    else
      {:error, error} ->
        Logger.warn("There was an error while retrieving the schema: #{Exception.message(error)}")
        nil
    end
  end
end
