defmodule ContainerProvider.Docker.Source do
  use GenServer
  require Logger
  alias ContainerProvider.HttpClient
  alias ContainerProvider.Docker.Client
  alias ContainerProvider.Labels
  alias Common.Events.{Down, Up}
  alias Common.Service.{Grpc, OpenApi, Metadata}

  def start_link(uri), do: GenServer.start_link(__MODULE__, uri, name: __MODULE__)

  defp services_behaviour, do: Application.fetch_env!(:services, :behaviour)

  @impl true
  def init(uri), do: {:ok, uri, {:continue, :wait_for_ra}}

  @impl true
  def handle_continue(:wait_for_ra, uri) do
    if Services.Cluster.running?() do
      with {:ok, pid} <- Client.start_link(uri),
           {:ok, client} <- Client.start_link(uri) do
        _ =
          Client.events(
            pid,
            ~s/since=0&filters={"event":["start","stop"],"type":["container"]}/,
            self()
          )

        Logger.debug("Docker source started")
        {:noreply, client}
      else
        {:error, reason} ->
          {:stop, reason}
      end
    else
      Process.sleep(1_000)

      {:noreply, uri, {:continue, :wait_for_ra}}
    end
  end

  @impl true
  def handle_info({:stream, {:data, data}}, client) do
    events =
      case data do
        %{"status" => "start", "id" => id} -> handle_service_up(id, client)
        %{"status" => "stop", "id" => id} -> [%Down{id: id}]
        _ -> []
      end

    services_behaviour().dispatch_events(events)
    {:noreply, client}
  end

  def handle_info(_, state), do: {:noreply, state}

  defp handle_service_up(id, client) do
    with {:ok,
          %{
            status: 200,
            data: %{
              "Config" => %{"Labels" => labels},
              "NetworkSettings" => %{"Networks" => networks}
            }
          }} <- Client.get(client, "/containers/#{id}/json"),
         ip <-
           networks
           |> Enum.map(fn {_, %{"IPAddress" => ip_address}} -> ip_address end)
           |> Enum.at(0),
         [service_name: service_name, openapi: openapi, grpc: grpc] <- find_endpoint(labels, ip) do
      [
        retrieve_specification(id, service_name, openapi),
        retrieve_specification(id, service_name, grpc)
      ]
      |> Enum.reject(&is_nil/1)
      |> Enum.map(&%Up{service: &1})
    else
      _ -> []
    end
  end

  defp retrieve_specification(_, _, nil), do: nil

  defp retrieve_specification(id, service_name, endpoint: endpoint, use_proxy: use_proxy) do
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

  defp retrieve_specification(
         id,
         service_name,
         [address: ip, port: port, use_tls: use_tls] = server
       ) do
    protocol = if use_tls, do: "https", else: "http"
    address = "#{ip}:#{port}"
    endpoint = "#{protocol}://#{address}"

    with {:ok, schema} <- UGRPC.Reflection.load_schema(endpoint) do
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

  defp find_endpoint(_, nil), do: nil

  defp find_endpoint(labels, ip) do
    with %Labels{service_name: service_name, openapi: openapi, grpc: grpc} <-
           Labels.from_map(labels) do
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
  end
end
