defmodule UnisonUI.SelfSpecificationServer do
  use GenServer, restart: :temporary
  require Services

  alias Services.{Event, Service}

  @openapi_specification File.read!("priv/openapi.yaml")
  @asyncapi_specification File.read!("priv/asyncapi.yaml")

  @spec start_link(term()) :: GenServer.on_start()
  def start_link(_opts), do: GenServer.start_link(__MODULE__, :ok, name: __MODULE__)

  @impl true
  def init(:ok), do: Services.init_wait_for_storage(self_specifications?())

  Services.wait_for_storage do
    send(self(), :publish)
    {:noreply, state}
  end

  @impl true
  def handle_info(:publish, true) do
    _ =
      Services.dispatch_events([
        %Event.Up{
          service: %Service.AsyncApi{
            id: "unisonui:unisonui-asyncapi",
            name: "UnisonUI",
            content: @asyncapi_specification,
            metadata: %Service.Metadata{provider: "unisonui", file: "asyncapi.yaml"}
          }
        },
        %Event.Up{
          service: %Service.OpenApi{
            id: "unisonui:unisonui-openapi",
            name: "UnisonUI",
            content: @openapi_specification,
            use_proxy: false,
            metadata: %Service.Metadata{provider: "unisonui", file: "openapi.yaml"}
          }
        }
      ])

    {:stop, :normal, true}
  end

  def handle_info(:publish, false), do: {:stop, :normal, false}

  defp self_specifications?, do: Application.fetch_env!(:unison_ui, :self_specification)
end
