defmodule GRPC.ClientSupervisor do
  use GenServer

  def start_link(_args), do: GenServer.start_link(__MODULE__, nil, name: __MODULE__)

  @impl true
  def init(_args), do: {:ok, %{}}

  @spec spawn_client(server :: String.t()) :: {:ok, GRPC.Client.Connection.t()} | {:error, term()}

  def spawn_client(server) do
    try do
      GenServer.call(__MODULE__, {:spawn_client, server})
    catch
      :exit, {e, _} -> {:error, e}
    end
  end

  @impl true
  def handle_call({:spawn_client, server}, _from, state) do
    case Map.get(state, server) do
      nil ->
        case start_child(server) do
          {:ok, pid} ->
            stream = %GRPC.Client.Connection{pid: pid}
            {:reply, {:ok, stream}, Map.put(state, server, stream)}

          error ->
            {:reply, error, state}
        end

      stream ->
        {:reply, {:ok, stream}, state}
    end
  end

  @impl true
  def handle_info({:DOWN, _ref, :process, pid, _reason}, state),
    do:
      {:noreply,
       state
       |> Stream.reject(fn {_, %GRPC.Client.Connection{pid: p}} -> p == pid end)
       |> Enum.into(%{})}

  defp start_child(server) do
    case GRPC.Client.start_link(server) do
      {:ok, pid} ->
        _ = Process.unlink(pid)
        _ = Process.monitor(pid)
        {:ok, pid}

      error ->
        error
    end
  end
end
