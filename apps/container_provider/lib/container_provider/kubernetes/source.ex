defmodule ContainerProvider.Kubernetes.Source do
  use Clustering.GlobalServer
  alias ContainerProvider.Labels
  require Logger
  @impl true
  def init(polling_interval) do
    try do
      conn = K8s.Conn.from_service_account()
      {:ok, {conn, %{}, polling_interval}, {:continue, :wait_for_ra}}
    rescue
      e -> {:stop, e}
    end
  end

  @impl true
  def handle_continue(:wait_for_ra, state) do
    if Services.Cluster.running?() do
      send(self(), :list_services)
      {:noreply, state}
    else
      Process.sleep(1_000)

      {:noreply, state, {:continue, :wait_for_ra}}
    end
  end

  def handle_info(:list_services, {conn, services, polling_interval} = state) do
    operation = K8s.Client.list("v1", "Service", namespace: :all)

    case K8s.Client.run(operation, conn) do
      {:ok, %{"items" => new_services}} ->
        new_services
        |> Enum.group_by(fn %{"metadata" => %{"namespace" => namespace}} -> namespace end)
        |> Stream.map(fn {namespace, new_services} ->
          new_services
          |> Enum.flat_map(fn %{"metadata" => %{"labels" => labels}} = service ->
            case Labels.from_map(labels) do
              nil -> []
              labels -> [{service, labels}]
            end
          end)
        end)

      {:error, reason} ->
        reason =
          if Exception.exception?(reason), do: Exception.message(reason), else: inspect(reason)

        Logger.warn("Error while fetching services: #{reason}")
        state
    end

    Process.send(self(), :list_services, polling_interval)
    state
  end
end
