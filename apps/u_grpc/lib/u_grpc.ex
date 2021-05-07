defmodule UGRPC do
  defdelegate new_client(server), to: UGRPC.ClientSupervisor
end
