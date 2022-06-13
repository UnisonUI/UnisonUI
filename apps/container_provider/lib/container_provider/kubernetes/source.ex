defmodule ContainerProvider.Kubernetes.Source do
  use Clustering.GlobalServer
  alias ContainerProvider.{Labels, Specifications}
  alias Services.Event
  require Logger
  require OK
  require Services

  @impl true
  def init(polling_interval), do: Services.init_wait_for_storage(polling_interval)

  Services.wait_for_storage do
    case K8s.Conn.from_service_account() do
      {:ok, conn} ->
        send(self(), :list_services)
        {:noreply, {conn, MapSet.new(), state}}

      {:error, reason} ->
        reason = if is_exception(reason), do: Exception.message(reason), else: inspect(reason)
        Logger.warn("Kubernetes source failed to start: #{reason}")
        {:stop, :normal, state}
    end
  end

  @impl true
  def handle_info(:list_services, {conn, services, polling_interval} = state) do
    operation = K8s.Client.list("v1", "Service", namespace: :all)

    state =
      case K8s.Client.run(conn, operation) do
        {:ok, %{"items" => new_services}} ->
          filtered_services =
            Stream.flat_map(new_services, &extract_service/1)
            |> Enum.into(%{}, fn service -> {service[:id], service} end)

          filtered_services_set = Enum.into(filtered_services, MapSet.new(), &elem(&1, 0))

          {services, events} =
            MapSet.difference(services, filtered_services_set)
            |> Enum.reduce({services, []}, fn id, {services, events} ->
              {MapSet.delete(services, id), [%Event.Down{id: id} | events]}
            end)

          {services, events} =
            MapSet.difference(filtered_services_set, services)
            |> Enum.reduce({services, events}, fn id, {services, events} ->
              %{
                service_name: service_name,
                openapi: openapi,
                asyncapi: asyncapi,
                grpc: grpc
              } = Map.get(filtered_services, id)

              events =
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
                |> Enum.reduce(events, &[&1 | &2])

              {MapSet.put(services, id), events}
            end)

          _ = Services.dispatch_events(Enum.reverse(events))
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

    Process.send_after(self(), :list_services, polling_interval)
    {:noreply, state}
  end

  defp extract_service(%{
         "metadata" => %{"labels" => labels, "uid" => id},
         "specs" => %{"clusterIP" => ip}
       }) do
    labels = Labels.from_map(labels)

    if Labels.valid?(labels) do
      [service_name: service_name, openapi: openapi, asyncapi: asyncapi, grpc: grpc] =
        Labels.extract_endpoint(labels, ip)

      [%{id: id, service_name: service_name, openapi: openapi, asyncapi: asyncapi, grpc: grpc}]
    else
      []
    end
  end
end
