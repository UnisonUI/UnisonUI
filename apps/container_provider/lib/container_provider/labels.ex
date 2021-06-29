defmodule ContainerProvider.Labels do
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

  defp labels, do: Application.fetch_env!(:container_provider, :labels)
end
