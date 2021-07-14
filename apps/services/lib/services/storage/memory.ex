defmodule Services.Storage.Memory do
  alias Services.Storage.Memory.Server
  @behaviour Services.Behaviour

  @spec alive?() :: boolean()
  def alive?, do: true

  @spec available_services :: {:ok, [Common.Service.t()]} | {:error, term()}
  defdelegate available_services, to: Server

  @spec service(id :: String.t()) :: {:ok, Common.Service.t()} | {:error, term()}
  defdelegate service(id), to: Server

  @spec dispatch_events(event :: [Common.Events.t()]) :: :ok | {:error, :timeout | term()}
  defdelegate dispatch_events(events), to: Server
end
