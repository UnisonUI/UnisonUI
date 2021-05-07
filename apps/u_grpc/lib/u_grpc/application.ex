defmodule UGRPC.Application do
  @moduledoc false

  use Application

  @impl true
  def start(_type, _args) do
    children = [
      UGRPC.ClientSupervisor,
      UGRPC.Reflection.Supervisor
    ]

    opts = [strategy: :one_for_one, name: UGRPC.Supervisor]
    Supervisor.start_link(children, opts)
  end
end
