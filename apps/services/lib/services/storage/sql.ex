defmodule Services.Storage.Sql do
  alias Services.Storage.Memory.Server
  @behaviour Services.Storage

  @spec alive?() :: true
  def alive?, do: true

  @spec available_services :: {:ok, [Services.t()]} | {:error, term()}
  defdelegate available_services, to: Server

  @spec service(id :: String.t()) :: {:ok, Services.t()} | {:error, term()}
  defdelegate service(id), to: Server

  @spec dispatch_events(event :: [Services.Event.t()]) :: :ok | {:error, :timeout | term()}
  defdelegate dispatch_events(events), to: Server
end
