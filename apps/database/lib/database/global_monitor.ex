defmodule Database.GlobalMonitor do
  use GenServer

  @moduledoc """
  Monitor Global Jobs
  """

  def start_link(_args), do:
    GenServer.start_link(__MODULE__, :ok, name: __MODULE__)

  @impl true
  def init(:ok), do:
    {:ok, %{}}

  def monitor(module, args \\ :ok), do:
    GenServer.call(__MODULE__, {:monitor, module, args})

  @impl true
  def handle_call({:monitor, module, args}, _from, state) do
    case do_monitor(module, args) do
      {:ok, {pid, ref}} ->
        {:reply, {:ok, pid}, Map.put_new(state, {pid, ref}, {module, args})}

      error ->
        {:reply, error, state}
    end
  end

  @impl true
  def handle_info({:DOWN, ref, :process, pid, _reason}, state) do
    case Map.fetch(state, {pid, ref}) do
      {:ok, {module, args}} ->
        state = Map.delete(state, {pid, ref})

        case do_monitor(module, args) do
          {:ok, pid_ref} ->
            {:noreply, Map.put_new(state, pid_ref, {module, args})}

          _error ->
            {:noreply, state}
        end

      :error ->
        {:noreply, state}
    end
  end

  @impl true
  def terminate(_reason, state) do
    state
    |> Enum.each(fn {{_pid, ref}, _} ->
      Process.demonitor(ref)
    end)

    :ok
  end

  defp do_monitor(module, args) do
    case start_global(module, args) do
      {:ok, pid} ->
        ref = Process.monitor(pid)
        {:ok, {pid, ref}}

      error ->
        error
    end
  end

  defp start_global(module, args),
    do:
      :global.trans(
        {module, module},
        fn ->
          case GenServer.start(module, args, name: {:global, module}) do
            {:ok, pid} ->
              {:ok, pid}

            {:error, {:already_started, pid}} ->
              {:ok, pid}

            error ->
              error
          end
        end,
        Node.list([:visible, :this])
      )
end
