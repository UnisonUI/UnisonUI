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

    [{Finch, name: FinchHttpClient}] ++
      docker_children(docker_config[:enabled] && docker_config[:host])
  end

  defp docker_children(host) when is_binary(host), do: [{ContainerProvider.Docker.Source, host}]

  defp docker_children(_) do
    Logger.info("Docker provider has been disabled")
    []
  end
end
