defmodule ContainerProvider.Kubernetes.Source do
  use Clustering.GlobalServer
  alias ContainerProvider.Labels
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
      case K8s.Client.run(operation, conn) do
        {:ok, %{"items" => new_services}} ->
          new_services
          |> Enum.group_by(fn %{"metadata" => %{"namespace" => namespace}} -> namespace end)
          |> Stream.map(fn {namespace, new_services} ->
            filtered_services =
              Enum.reject(new_services, fn %{"metadata" => %{"labels" => labels}} ->
                is_nil(Labels.from_map(labels))
              end)

            services = services[namespace] || filtered_services

            services
            |> Stream.reject(fn service -> Enum.any?(filtered_services, &(&1 == service)) end)
            |> Stream.flat_map(fn %{"metadata" => %{"labels" => labels}} = service ->
              case Labels.from_map(labels) do
                nil ->
                  []

                labels ->
                  nil
              end
            end)
          end)

          state

        {:error, reason} ->
          reason =
            if Exception.exception?(reason), do: Exception.message(reason), else: inspect(reason)

          Logger.warn("Error while fetching services: #{reason}")
          state
      end

    Process.send(self(), :list_services, polling_interval)
    {:noreply, state}
  end
end
