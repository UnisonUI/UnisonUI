defmodule UGRPC.Reflection do
  @spec load_schema(server :: String.t()) :: {:ok, UGRPC.Protobuf.Structs.Schema.t()} | {:error, term()}
  defdelegate load_schema(server), to: UGRPC.Reflection.Supervisor
end
