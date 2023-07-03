defmodule Services.Storage.Raft do
  require Logger
  alias Services.State

  @dialyzer :no_return
  @behaviour Services.Storage

  @spec alive?() :: boolean()
  def alive?, do: Services.Storage.Raft.Cluster.running?()

  @spec available_services :: {:ok, [Services.t()]} | {:error, term()}
  def available_services do
    case :ra.local_query(ra_server_id(), &State.available_services/1) do
      {:ok, {_, services}, _} -> {:ok, services}
      {:timeout, _} -> {:error, :timeout}
      error -> error
    end
  end

  @spec service(id :: String.t()) :: {:ok, Services.t()} | {:error, term()}
  def service(id) do
    case :ra.local_query(ra_server_id(), &State.service(&1, id)) do
      {:ok, {_, nil}, _} -> {:error, :not_found}
      {:ok, {_, service}, _} -> {:ok, service}
      {:timeout, _} -> {:error, :timeout}
      error -> error
    end
  end

  @spec dispatch_events(event :: [Services.Event.t()]) :: :ok | {:error, :timeout | term()}
  def dispatch_events(events) do
    case :ra.process_command(ra_server_id(), {:events, events}) do
      {:error, reason} ->
        {:error, reason}

      {:timeout, _} ->
        {:error, :timeout}

      _ ->
        :ok
    end
  end

  defp ra_server_id, do: {:unisonui, node()}
end
