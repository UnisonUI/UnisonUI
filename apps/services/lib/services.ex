defmodule Services do
  require Logger
  alias Common.Events
  @behaviour Services.Behaviour

  @spec available_services :: {:ok, [Common.Service.t()]} | {:error, term()}
  def available_services do
    case :ra.local_query(
           :unisonui,
           &Enum.into(&1, [], fn {_, service} -> %Events.Up{service: service} end)
         ) do
      {:ok, {_, services}, _} -> {:ok, services}
      {:error, _} = error -> error
      {:timeout, _} -> {:error, :timeout}
    end
  end

  @spec service(id :: String.t()) :: {:ok, Common.Service.t()} | {:error, term()}
  def service(id) do
    case :ra.local_query(:unisonui, &Map.get(&1, id, :not_found)) do
      {:ok, {_, :not_found}, _} -> {:error, :not_found}
      {:ok, {_, service}, _} -> {:ok, service}
      {:error, _} = error -> error
      {:timeout, _} -> {:error, :timeout}
    end
  end

  @spec dispatch_events(event :: [Common.Events.t()]) :: :ok | {:error, :timeout | term()}
  def dispatch_events(events),
    do:
      Enum.reduce_while(events, :ok, fn event, _ ->
        case dispatch_event(event) do
          :ok -> {:cont, :ok}
          error -> {:halt, error}
        end
      end)

  @spec dispatch_event(event :: Common.Events.t()) :: :ok | {:error, :timeout | term()}
  defp dispatch_event(event) do
    case :ra.process_command(:unisonui, {:event, event}) do
      {:error, reason} ->
        Logger.warn("Couldn't dispatch #{inspect(event)} because #{inspect(reason)}")
        {:error, reason}

      {:timeout, _} ->
        Logger.warn("Couldn't dispatch #{inspect(event)} because it could not be replicated")
        {:error, :timeout}

      _ ->
        :ok
    end
  end
end
