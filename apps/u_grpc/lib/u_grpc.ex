defmodule GRPC do
  defdelegate new_client(server), to: GRPC.ClientSupervisor
end
