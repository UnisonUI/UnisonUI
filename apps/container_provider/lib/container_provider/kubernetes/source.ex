defmodule ContainerProvider.Kubernetes.Source do
  use Clustering.GlobalServer
  alias ContainerProvider.{Labels, Specifications}
  alias Common.Events
  require Logger
  require OK

  defp services_storage, do: Application.fetch_env!(:services, :storage_backend)

  @impl true
  def init(polling_interval) do
    try do
      conn = K8s.Conn.from_service_account()
      {:ok, {conn, %{}, polling_interval}, {:continue, :wait_for_storage}}
    rescue
      e -> {:stop, {:stop, e}}
    end
  end

  @impl true
  def handle_continue(:wait_for_storage, state) do
    if services_storage().running?() do
      send(self(), :list_services)
      {:noreply, state}
    else
      Process.sleep(1_000)

      {:noreply, state, {:continue, :wait_for_storage}}
    end
  end

  @impl true
  def handle_info(:list_services, {conn, services, polling_interval} = state) do
    operation = K8s.Client.list("v1", "Service", namespace: :all)

    state =
      case K8s.Client.run(conn, operation) do
        {:ok, %{"items" => new_services}} ->
          {events, services} =
            new_services
            |> Enum.group_by(fn %{"metadata" => %{"namespace" => namespace}} -> namespace end)
            |> Enum.reduce({[], services}, fn {namespace, new_services},
                                              {events, services_with_namespace} ->
              {events, services} = handle_services(services[namespace], events, new_services)

              events =
                Enum.reduce(services, events, fn %{
                                                   "metadata" => %{
                                                     "labels" => labels,
                                                     "uid" => id
                                                   },
                                                   "specs" => %{"clusterIP" => ip}
                                                 },
                                                 events ->
                  [service_name: service_name, openapi: openapi, grpc: grpc] =
                    labels |> Labels.from_map() |> Labels.extract_endpoint(ip)

                  [
                    Specifications.retrieve_specification(id, service_name, openapi),
                    Specifications.retrieve_specification(id, service_name, grpc)
                  ]
                  |> Enum.reject(&is_nil/1)
                  |> Enum.map(&%Events.Up{service: &1})
                  |> Enum.reduce(events, &[&1 | &2])
                end)

              {events, Map.put(services_with_namespace, namespace, services)}
            end)

          services_storage().dispatch_events(events)
          {conn, services, polling_interval}

        {:error, reason} ->
          reason =
            case reason do
              %K8s.Middleware.Error{error: error} -> error
              reason -> reason
            end

          reason =
            if Exception.exception?(reason), do: Exception.message(reason), else: inspect(reason)

          Logger.warn("Error while fetching services: #{reason}")
          state
      end

    Process.send(self(), :list_services, polling_interval)
    {:noreply, state}
  end

  defp handle_services(nil, events, filtered_services), do: {events, filtered_services}

  defp handle_services(services, events, filtered_services) do
    events =
      services
      |> Stream.reject(fn service -> Enum.any?(filtered_services, &(&1 == service)) end)
      |> Enum.reduce(events, fn %{"metadata" => %{"uid" => uid}}, events ->
        [%Events.Down{id: uid} | events]
      end)

    services =
      Enum.reject(filtered_services, fn service -> Enum.any?(services, &(&1 == service)) end)

    {events, services}
  end
end
