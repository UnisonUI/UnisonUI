defmodule GRPC.Reflection.Supervisor do
  use DynamicSupervisor

  def start_link(_), do: DynamicSupervisor.start_link(__MODULE__, nil, name: __MODULE__)

  def init(_), do: DynamicSupervisor.init(strategy: :one_for_one)

  @spec load_schema(server :: String.t()) ::
          {:ok, GRPC.Protobuf.Structs.Schema.t()} | {:error, term()}
  def load_schema(server) do
    with {:ok, pid} <- DynamicSupervisor.start_child(__MODULE__, GRPC.Reflection.Worker) do
      GRPC.Reflection.Worker.load_schema(pid, server)
    else
      error -> error
    end
  end
end
