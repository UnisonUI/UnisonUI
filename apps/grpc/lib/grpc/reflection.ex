defmodule GRPC.Reflection do
  @spec load_schema(server :: String.t()) :: {:ok, Protobuf.Structs.Schema.t()} | {:error, term()}
  defdelegate load_schema(server), to: GRPC.Reflection.Supervisor
end
