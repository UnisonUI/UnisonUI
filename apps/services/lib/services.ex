defmodule Services do
  @type t ::
          Services.Service.AsyncApi.t() | Services.Service.OpenApi.t() | Services.Service.Grpc.t()

  defp storage_backend, do: Application.fetch_env!(:services, :storage_backend)

  @spec alive?() :: boolean
  def alive? do
    try do
      storage_backend().alive?()
    catch
      :exit, _ -> false
    end
  end

  @spec available_services :: {:ok, [t()]} | {:error, term()}
  def available_services, do: storage_backend().available_services()

  @spec service(id :: String.t()) :: {:ok, t()} | {:error, term()}
  def service(id), do: storage_backend().service(id)

  @spec dispatch_events(events :: [Services.Event.t()]) :: :ok | {:error, term()}
  def dispatch_events(events), do: storage_backend().dispatch_events(events)

  def init_wait_for_storage(state, counter \\ 10),
    do: {:ok, {state, counter}, {:continue, :wait_for_storage}}

  defmacro wait_for_storage(do: block) do
    quote context: __CALLER__.module do
      @impl true
      def handle_continue(:wait_for_storage, var!(state)) do
        {state, counter} = var!(state)

        if Services.alive?() do
          unquote(block)
        else
          if counter > 0 do
            Process.sleep(1_000)
            {:noreply, {state, counter - 1}, {:continue, :wait_for_storage}}
          else
            {:stop, :storage_not_ready, state}
          end
        end
      end
    end
  end
end
