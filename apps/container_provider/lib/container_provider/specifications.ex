defmodule ContainerProvider.Specifications do
  alias ContainerProvider.HttpClient
  alias Services.Service
  require Logger

  def retrieve_specification(id, service_name, provider,
        type: type,
        endpoint: endpoint,
        use_proxy: use_proxy
      ) do
    with data when not is_nil(data) <- HttpClient.download_file(endpoint) do
      %URI{path: path} = URI.parse(endpoint)
      metadata = %Service.Metadata{provider: provider, file: String.slice(path, 1..-1)}

      fields = %{
        id: id,
        name: service_name,
        content: data,
        use_proxy: use_proxy,
        metadata: metadata
      }

      {struct, fields} =
        case type do
          :openapi -> {Service.OpenApi, fields}
          :asyncapi -> {Service.AsyncApi, Map.delete(fields, :use_proxy)}
        end

      struct!(struct, fields)
    end
  end

  def retrieve_specification(
        id,
        service_name,
        provider,
        [address: ip, port: port, use_tls: use_tls] = server
      ) do
    protocol = if use_tls, do: "https", else: "http"
    address = "#{ip}:#{port}"
    endpoint = "#{protocol}://#{address}"

    with {:ok, schema} <- GRPC.Reflection.load_schema(endpoint) do
      metadata = %Service.Metadata{provider: provider, file: address}

      %Service.Grpc{
        id: id,
        name: service_name,
        schema: schema,
        servers: %{address => struct(Service.Grpc.Server, server)},
        metadata: metadata
      }
    else
      {:error, error} ->
        Logger.warn("There was an error while retrieving the schema: #{Exception.message(error)}")
        nil
    end
  end

  def retrieve_specification(_, _, _, _), do: nil
end
