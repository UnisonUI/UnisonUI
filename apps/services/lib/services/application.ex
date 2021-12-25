defmodule Services.Application do
  @moduledoc false

  use Application

  @impl true
  def start(_type, _args) do
    children = [
      {Task.Supervisor, name: Services.TaskSupervisor},
      {Services.Aggregator, []},
      {storage_supervisor(), []}
    ]

    opts = [strategy: :one_for_one, name: Services.Supervisor]
    Supervisor.start_link(children, opts)
  end

  defp storage_supervisor do
    backend = Application.fetch_env!(:services, :storage_backend) |> to_string()
    :"#{backend}.Supervisor"
  end
end
