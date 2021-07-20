defmodule Services.Storage do
  @callback alive?() :: boolean
  @callback available_services :: {:ok, [Services.t()]} | {:error, term()}
  @callback service(id :: String.t()) :: {:ok, Services.t()} | {:error, term()}
  @callback dispatch_events(events :: [Services.Event.t()]) :: :ok | {:error, term()}
end
