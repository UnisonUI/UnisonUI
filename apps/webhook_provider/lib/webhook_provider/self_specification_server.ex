defmodule WebhookProvider.SelfSpecificationServer do
  use GenServer
  require Services

  alias Services.{Event, Metadata, OpenApi}

  @specification File.read!("priv/webhook-specification.yaml")

  @spec start_link(term()) :: GenServer.on_start()
  def start_link(_opts), do: GenServer.start_link(__MODULE__, :ok, name: __MODULE__)

  @impl true
  def init(:ok), do: {:ok, self_specifications?(), {:continue, :wait_for_storage}}

  Services.wait_for_storage do
    send(self(), :publish)
    {:noreply, state}
  end

  @impl true
  def handle_info(:publish, true) do
    Services.dispatch_events([
      %Event.Up{
        service: %OpenApi{
          id: "unisonui:webhook",
          name: "Webhook provider",
          content: @specification,
          use_proxy: false,
          metadata: %Metadata{provider: "webhook", file: "webhook-specification.yaml"}
        }
      }
    ])

    {:stop, :normal, true}
  end

  def handle_info(:publish, false), do: {:stop, :normal, false}

  defp self_specifications?, do: Application.fetch_env!(:webhook_provider, :self_specification)
end
