defmodule UGRPC.Reflection.Supervisor do
  use DynamicSupervisor

  def start_link(_), do: DynamicSupervisor.start_link(__MODULE__, nil, name: __MODULE__)

  def init(_), do: DynamicSupervisor.init(strategy: :one_for_one)

  @spec load_schema(server :: String.t()) :: {:ok, UGRPC.Protobuf.Structs.Schema.t()} | {:error, term()}
  def load_schema(server) do
    with {:ok, pid} <- DynamicSupervisor.start_child(__MODULE__, UGRPC.Reflection.Worker) do
      UGRPC.Reflection.Worker.load_schema(pid, server)
    else
      error -> error
    end
  end
end
