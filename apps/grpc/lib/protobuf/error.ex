defmodule Protobuf.ProtocError do
  defexception [:message]
  @impl true
  def exception(message), do: %__MODULE__{message: message}
end

defmodule Protobuf.UnknownMessageError do
  defexception [:message]
  @impl true
  def exception(type), do: %__MODULE__{message: "unknown message: #{type}"}
end

defmodule Protobuf.UnknownFieldError do
  defexception [:message]

  def exception(id, type), do: exception("Field #{id} not found for #{type}")

  @impl true
  def exception(message), do: %__MODULE__{message: message}
end

defmodule Protobuf.RequiredFieldError do
  defexception [:message]
  @impl true
  def exception(field), do: %__MODULE__{message: "#{field} is required"}
end
