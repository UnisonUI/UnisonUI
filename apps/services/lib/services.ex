defmodule Services do
  require Logger
  @behaviour Services.Behaviour

  @spec available_services :: [Common.Service.t()]
  defdelegate available_services, to: Services.Aggregator

  @spec service(id :: String.t()) :: {:ok, Common.Service.t()} | {:error, :not_found}
  defdelegate service(id), to: Services.Aggregator

  @spec dispatch_event(event :: Common.Events.t()) :: :ok
  def dispatch_event(event) do
    case Database.transaction(fn -> Database.Schema.Events.insert(event) end) do
      {:error, reason} ->
        Logger.warn("Couldn't dispatch #{inspect(event)} because #{inspect(reason)}")

      _ ->
        nil
    end

    :ok
  end
end
