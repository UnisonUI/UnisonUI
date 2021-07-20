defmodule ContainerProvider.Docker.Source do
  use GenServer
  require Logger
  alias ContainerProvider.Docker.{GetClient, EventsClient}
  alias ContainerProvider.{Labels,Specifications}
  alias Common.Events.{Down, Up}

  def start_link(uri), do: GenServer.start_link(__MODULE__, uri, name: __MODULE__)

  defp services_storage, do: Application.fetch_env!(:services, :storage_backend)
  defp connection_backoff, do: Application.get_env(:container_provider, :connection_backoff, [])
  defp connection_backoff_start, do: connection_backoff()[:start] || 0
  defp connection_backoff_interval, do: connection_backoff()[:interval] || 1_000
  defp connection_backoff_max, do: connection_backoff()[:max] || 5_000

  @impl true
  def init(uri), do: {:ok, uri, {:continue, :wait_for_storage}}

  @impl true
  def handle_continue(:wait_for_storage, uri) do
    if services_storage().alive?() do
      with {:ok, _} <- EventsClient.start_link(uri),
           {:ok, _} <- GetClient.start_link(uri) do
        _ = watch_events()
        Logger.debug("Docker source started")
        {:noreply, connection_backoff_start()}
      else
        {:error, reason} ->
          {:stop, reason}
      end
    else
      Process.sleep(1_000)

      {:noreply, uri, {:continue, :wait_for_storage}}
    end
  end

  @impl true
  def handle_info({:stream, {:status, status}}, _state) when status < 400,
    do: {:noreply, connection_backoff_start()}

  def handle_info({:stream, {:data, data}}, state) do
    events =
      case data do
        %{"status" => "start", "id" => id} ->
          handle_service_up(id)

        %{"status" => "stop", "id" => id} ->
          [%Down{id: id}]

        _ ->
          []
      end

    services_storage().dispatch_events(events)
    {:noreply, state}
  end

  def handle_info({:stream, {:error, error}}, timeout) do
    reason = if Exception.exception?(error), do: Exception.message(error), else: error
    Logger.warn(reason)

    state = if Exception.exception?(error), do: reconnect(timeout), else: timeout
    {:noreply, state}
  end

  def handle_info({:stream, :done}, timeout), do: {:noreply, reconnect(timeout)}

  def handle_info(_, state), do: {:noreply, state}

  defp reconnect(timeout) do
    Process.sleep(timeout)
    _ = EventsClient.reconnect()
    _ = watch_events()
    min(timeout + connection_backoff_interval(), connection_backoff_max())
  end

  defp watch_events,
    do:
      EventsClient.request(
        ~s(/events?since=0&filters={"event":["start","stop"],"type":["container"]}),
        self()
      )

  defp handle_service_up(id) do
    with {:ok,
          %{
            status: 200,
            data: %{
              "Config" => %{"Labels" => labels},
              "NetworkSettings" => %{"Networks" => networks}
            }
          }} <- GetClient.request("/containers/#{id}/json"),
         ip <-
           networks
           |> Enum.map(fn {_, %{"IPAddress" => ip_address}} -> ip_address end)
           |> Enum.at(0),
         [service_name: service_name, openapi: openapi, grpc: grpc] <- Labels.extract_endpoint(labels, ip) do
      [
        Specifications.retrieve_specification(id, service_name, openapi),
        Specifications.retrieve_specification(id, service_name, grpc)
      ]
      |> Enum.reject(&is_nil/1)
      |> Enum.map(&%Up{service: &1})
    else
      _ -> []
    end
  end

end
