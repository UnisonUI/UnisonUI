defmodule Services do
  @type t ::
          Services.Service.AsyncApi.t() | Services.Service.OpenApi.t() | Services.Service.Grpc.t()

  defp storage_backend, do: Application.fetch_env!(:services, :storage_backend)

  @spec alive?() :: boolean
  def alive?, do: storage_backend().alive?()

  @spec available_services :: {:ok, [t()]} | {:error, term()}
  def available_services, do: storage_backend().available_services()

  @spec service(id :: String.t()) :: {:ok, t()} | {:error, term()}
  def service(id), do: storage_backend().service(id)

  @spec dispatch_events(events :: [Services.Event.t()]) :: :ok | {:error, term()}
  def dispatch_events(events), do: storage_backend().dispatch_events(events)

  defmacro wait_for_storage(do: block) do
    quote context: __CALLER__.module do
      @impl true
      def handle_continue(:wait_for_storage, var!(state)) do
        if Services.alive?() do
          unquote(block)
        else
          Process.sleep(1_000)
          {:noreply, var!(state), {:continue, :wait_for_storage}}
        end
      end
    end
  end
end
