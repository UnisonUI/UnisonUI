defmodule GRPC.Error do
  defexception [:status, :message]
  @type t :: %__MODULE__{status: GRPC.Status.t(), message: String.t()}
  def new(status, message) do
    %__MODULE__{status: status, message: message}
  end
end
