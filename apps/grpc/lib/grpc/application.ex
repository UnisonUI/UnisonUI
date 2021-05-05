defmodule GRPC.Application do
  @moduledoc false

  use Application

  @impl true
  def start(_type, _args) do
    children = [
      GRPC.ClientSupervisor,
      GRPC.Reflection.Supervisor
    ]

    opts = [strategy: :one_for_one, name: GRPC.Supervisor]
    Supervisor.start_link(children, opts)
  end
end
