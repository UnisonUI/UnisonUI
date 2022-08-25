defmodule Services do
  require OK

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

  @spec available_services_by_provider(provider :: String.t()) :: {:ok, [t()]} | {:error, term()}
  def available_services_by_provider(provider) do
    OK.for do
      services <- available_services()
      filtered_services = filter_services_by_provider(services, provider)
    after
      filtered_services
    end
  end

  defp filter_services_by_provider(services, provider) do
    Enum.filter(services, fn
      %{metadata: %Services.Service.Metadata{provider: ^provider}} ->
        true

      _ ->
        false
    end)
  end

  @spec service(id :: String.t()) :: {:ok, t()} | {:error, term()}
  def service(id), do: storage_backend().service(id)

  @spec dispatch_events(events :: [Services.Event.t()]) :: :ok | {:error, term()}
  def dispatch_events(events), do: storage_backend().dispatch_events(events)

  defmacro init_wait_for_storage(state, counter \\ 10) do
    quote do
      {:ok, {unquote(state), unquote(counter)}, {:continue, :wait_for_storage}}
    end
  end

  defmacro wait_for_storage(do: block) do
    quote context: __CALLER__.module do
      @impl true
      def handle_continue(:wait_for_storage, var!(state)) do
        {state, counter} = var!(state)

        if Services.alive?() do
          var!(state) = state
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
