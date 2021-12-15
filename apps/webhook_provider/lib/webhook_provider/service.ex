defmodule WebhookProvider.Service do
  alias Services.{AsyncApi, Grpc, OpenApi, Metadata}

  def from_map(service) do
    decode_asyncopenapi(service) || decode_grpc(service)
  end

  defp decode_asyncopenapi(service) do
    with name when not is_nil(name) <- service["name"],
         specifications when not is_nil(specifications) <- service["specification"],
         type when type in ["asyncapi", "openapi"] <- service["type"] do
      service = %{
        id: id(name),
        name: name,
        content: specifications,
        metadata: metadata(name)
      }

      type =
        case type do
          "asyncapi" -> AsyncApi
          "openapi" -> OpenApi
        end

      struct(type, service)
    end
  end

  defp decode_grpc(service) do
    with name when not is_nil(name) <- service["name"],
         schema when not is_nil(schema) <- service["schema"],
         servers when is_map(servers) <- service["servers"] do
      %Grpc{
        id: id(name),
        name: name,
        schema: schema,
        servers: servers,
        metadata: metadata(name)
      }
    end
  end

  def id(service_name), do: "webhook:#{service_name}"

  defp metadata(service_name), do: %Metadata{provider: "webhook", file: service_name}
end
