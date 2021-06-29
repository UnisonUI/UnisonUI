defmodule Services.Behaviour do
  @callback available_services :: {:ok,[Common.Service.t()]}|{:error,term()}
  @callback service(id :: String.t()) :: {:ok, Common.Service.t()} | {:error, term()}
  @callback dispatch_events(events :: [Common.Events.t()]) :: :ok | {:error, term()}
end
