defmodule ContainerProvider.Docker.Source do
  use GenServer, restart: :transient
  require Logger
  require Services
  alias ContainerProvider.Docker.{GetClient, EventsClient}
  alias ContainerProvider.{Labels, Specifications}
  alias Services.Event

  def start_link(uri), do: GenServer.start_link(__MODULE__, uri, name: __MODULE__)

  defp connection_backoff, do: Application.get_env(:container_provider, :connection_backoff, [])
  defp connection_backoff_start, do: connection_backoff()[:start] || 0
  defp connection_backoff_interval, do: connection_backoff()[:interval] || 1_000
  defp connection_backoff_max, do: connection_backoff()[:max] || 5_000

  @impl true
  def init(uri) do
    Process.flag(:trap_exit, true)
    Services.init_wait_for_storage(uri)
  end

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
  def terminate(reason, _), do: reason

  @impl true
  def handle_info({:stream, {:status, status}}, _state) when status < 400,
    do: {:noreply, connection_backoff_start()}

  def handle_info({:stream, {:data, data}}, state) do
    events =
      case data do
        %{"status" => status, "id" => id, "Actor" => %{"Attributes" => labels}}
        when status in ["start", "stop"] ->
          case extract_labels(labels) do
            nil ->
              []

            labels ->
              handle_event(status, id, labels)
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

  defp handle_event("start", id, labels) do
    with {:ok,
          %{
            status: 200,
            data: %{"NetworkSettings" => %{"Networks" => networks}}
          }} <- GetClient.request("/containers/#{id}/json"),
         [ip | _] <- Enum.map(networks, fn {_, %{"IPAddress" => ip_address}} -> ip_address end),
         [service_name: service_name, openapi: openapi, asyncapi: asyncapi, grpc: grpc] <-
           Labels.extract_endpoint(labels, ip) do
      [
        Specifications.retrieve_specification(
          id,
          service_name,
          openapi && Keyword.put(openapi, :type, :openapi)
        ),
        Specifications.retrieve_specification(
          id,
          service_name,
          asyncapi && Keyword.put(asyncapi, :type, :asyncapi)
        ),
        Specifications.retrieve_specification(id, service_name, grpc)
      ]
      |> Enum.reject(&is_nil/1)
      |> Enum.map(&%Event.Up{service: &1})
    else
      _ -> []
    end
  end

  defp handle_event("stop", id, _labels), do: [%Event.Down{id: id}]

  defp extract_labels(labels) do
    labels = Labels.from_map(labels)

    unless Labels.valid?(labels) do
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
end
