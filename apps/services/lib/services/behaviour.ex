defmodule Services.Behaviour do
  @callback available_services :: [Common.Service.t()]
  @callback service(id :: String.t()) :: {:ok, Common.Service.t()} | {:error, :not_found}
  @callback dispatch_event(event :: Common.Events.t()) :: :ok
end
