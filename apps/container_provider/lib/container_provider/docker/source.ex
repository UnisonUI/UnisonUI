defmodule ContainerProvider.Docker.Source do
  use GenServer, restart: :transient
  require Logger
  require Services
  alias ContainerProvider.Docker.{GetClient, EventsClient}
  alias ContainerProvider.{Labels, Specifications}
  alias Services.Event.{Down, Up}

  def start_link(uri), do: GenServer.start_link(__MODULE__, uri, name: __MODULE__)

  defp connection_backoff, do: Application.get_env(:container_provider, :connection_backoff, [])
  defp connection_backoff_start, do: connection_backoff()[:start] || 0
  defp connection_backoff_interval, do: connection_backoff()[:interval] || 1_000
  defp connection_backoff_max, do: connection_backoff()[:max] || 5_000

  @impl true
  def init(uri), do: {:ok, uri, {:continue, :wait_for_storage}}

  Services.wait_for_storage do
    with {:ok, _} <- EventsClient.start_link(state),
         {:ok, _} <- GetClient.start_link(state) do
      _ = watch_events()
      Logger.debug("Docker source started")
      {:noreply, connection_backoff_start()}
    else
      {:error, reason} ->
        reason = if is_exception(reason), do: Exception.message(reason), else: inspect(reason)

        Logger.warn("Docker source failed to start: #{reason}")
        {:stop, :normal, state}
    end
  end

  @impl true
  def handle_info({:stream, {:status, status}}, _state) when status < 400,
    do: {:noreply, connection_backoff_start()}

  def handle_info({:stream, {:data, data}}, state) do
    events =
      case data do
        %{"status" => "start", "id" => id, "Actor" => %{"Attributes" => labels}} ->
          case extract_labels(labels) do
            nil ->
              []

            labels ->
              handle_service_up(id, labels)
          end

        %{"status" => "stop", "id" => id, "Actor" => %{"Attributes" => labels}} ->
          case extract_labels(labels) do
            nil ->
              []

            _ ->
              [%Down{id: id}]
          end

        _ ->
          []
      end

    _ = Services.dispatch_events(events)
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

  defp extract_labels(labels) do
    labels = Labels.from_map(labels)

    if is_nil(labels.openapi) and is_nil(labels.grpc) do
      nil
    else
      labels
    end
  end

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

  defp handle_service_up(id, labels) do
    with {:ok,
          %{
            status: 200,
            data: %{"NetworkSettings" => %{"Networks" => networks}}
          }} <- GetClient.request("/containers/#{id}/json"),
         ip <-
           networks
           |> Enum.map(fn {_, %{"IPAddress" => ip_address}} -> ip_address end)
           |> Enum.at(0),
         [service_name: service_name, openapi: openapi, grpc: grpc] <-
           Labels.extract_endpoint(labels, ip) do
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
