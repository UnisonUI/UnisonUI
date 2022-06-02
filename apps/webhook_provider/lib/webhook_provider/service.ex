defmodule WebhookProvider.Service do
  require Logger
  alias Services.Service

  def from_map(service) do
    decode_asyncopenapi(service) || decode_grpc(service)
  end

  defp decode_asyncopenapi(service) do
    with name when not is_nil(name) <- service["name"],
         specifications when not is_nil(specifications) <- service["specification"],
         type when type in ["asyncapi", "openapi"] <- String.downcase(service["type"] || "") do
      service = %{
        id: id(name),
        name: name,
        content: specifications,
        metadata: metadata(name)
      }

      type =
        case type do
          "asyncapi" -> Service.AsyncApi
          "openapi" -> Service.OpenApi
        end

      struct(type, service)
    end
  end

  defp decode_grpc(service) do
    with name when not is_nil(name) <- service["name"],
         schema when not is_nil(schema) <- service["schema"],
         servers when is_map(servers) <- service["servers"],
         dir when not is_nil(dir) <- System.tmp_dir(),
         file <- Path.join(dir, random_file()),
         :ok <- File.write(file, schema),
         {:ok, schema} <- GRPC.Protobuf.compile(file),
         _ <- File.rm(file) do
      %Service.Grpc{
        id: id(name),
        name: name,
        schema: schema,
        servers: servers,
        metadata: metadata(name)
      }
    else
      {:error, error} when is_exception(error) ->
        Logger.warn("Error while compile protobuf: #{Exception.message(error)}")
        nil

      {:error, error} ->
        Logger.warn("Error while compile protobuf: #{inspect(error)}")
        nil

      _ ->
        nil
    end
  end

  def id(service_name), do: "webhook:#{service_name}"

  defp metadata(service_name), do: %Service.Metadata{provider: "webhook", file: service_name}

  defp random_file,
    do:
      "unisonui_webhook_grpc_#{:crypto.strong_rand_bytes(16) |> Base.encode32(case: :lower, padding: false)}.proto"
end
