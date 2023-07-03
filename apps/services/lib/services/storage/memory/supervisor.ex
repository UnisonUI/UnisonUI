defmodule Services.Storage.Memory.Supervisor do
  use Supervisor

  @spec start_link(term()) :: Supervisor.on_start()
  def start_link(opts), do: Supervisor.start_link(__MODULE__, opts, name: __MODULE__)

  @impl true
  def init(_), do: Supervisor.init([{Services.Storage.Memory.Server, []}], strategy: :one_for_one)
end
