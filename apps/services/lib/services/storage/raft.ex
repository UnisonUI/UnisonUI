defmodule Services.Storage.Raft do
  require Logger
  alias Services.Event
  @dialyzer :no_return
  @behaviour Services.Storage

  @spec alive?() :: boolean()
  def alive?, do: Services.Storage.Raft.Cluster.running?()

  @spec available_services :: {:ok, [Services.t()]} | {:error, term()}
  def available_services do
    case :ra.local_query(
           :unisonui,
           &Enum.into(&1, [], fn {_, service} -> %Event.Up{service: service} end)
         ) do
      {:ok, {_, services}, _} -> {:ok, services}
      {:timeout, _} -> {:error, :timeout}
      error -> error
    end
  end

  @spec service(id :: String.t()) :: {:ok, Services.t()} | {:error, term()}
  def service(id) do
    case :ra.local_query(:unisonui, &Map.get(&1, id, :not_found)) do
      {:ok, {_, :not_found}, _} -> {:error, :not_found}
      {:ok, {_, service}, _} -> {:ok, service}
      {:timeout, _} -> {:error, :timeout}
      error -> error
    end
  end

  @spec dispatch_events(event :: [Services.Event.t()]) :: :ok | {:error, :timeout | term()}
  def dispatch_events(events),
    do:
      Enum.reduce_while(events, :ok, fn event, _ ->
        case dispatch_event(event) do
          :ok -> {:cont, :ok}
          error -> {:halt, error}
        end
      end)

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