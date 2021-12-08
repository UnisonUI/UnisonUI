defmodule GRPC.Client.Connection do
  @type t :: %__MODULE__{pid: pid(), ref: any()}
  defstruct [:pid, :ref]
end
