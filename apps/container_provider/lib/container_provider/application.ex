defmodule ContainerProvider.Application do
  @moduledoc false
  require Logger
  use Application

  @impl true
  def start(_type, _args) do
    opts = [strategy: :one_for_one, name: ContainerProvider.Supervisor]

    Application.fetch_env!(:container_provider, :enabled)
    |> child_spec()
    |> Supervisor.start_link(opts)
  end

  defp child_spec(false) do
    Logger.info("Container provider has been disabled")
    []
  end

  defp child_spec(_) do
    docker_config = Application.fetch_env!(:container_provider, :docker)
    kubernetes_config = Application.fetch_env!(:container_provider, :kubernetes)

    docker_children(docker_config[:enabled] && docker_config[:host]) ++
      kubernetes_children(kubernetes_config[:enabled] && kubernetes_config[:polling_interval])
  end

  defp docker_children(host) when is_binary(host), do: [{ContainerProvider.Docker.Source, host}]

  defp docker_children(_) do
    Logger.info("Docker provider has been disabled")
    []
  end

  defp kubernetes_children(nil) do
    Logger.info("Kubernetes provider has been disabled")
    []
  end

  defp kubernetes_children(polling_interval) do
    ContainerProvider.Kubernetes.Source.start_child(polling_interval)
    []
  end
end
