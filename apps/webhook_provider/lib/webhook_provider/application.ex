defmodule WebhookProvider.Application do
  @moduledoc false
  require Logger
  use Application

  @impl true
  def start(_type, _args) do
    opts = [strategy: :one_for_one, name: WebhookProvider.Supervisor]

    Application.fetch_env!(:webhook_provider, :enabled)
    |> child_spec()
    |> Supervisor.start_link(opts)
  end

  defp child_spec(true),
    do: [
      WebhookProvider.SelfSpecificationServer,
      Plug.Cowboy.child_spec(
        scheme: :http,
        plug: WebhookProvider.Routes,
        options: [port: Application.get_env(:webhook_provider, :port, 3000)]
      )
    ]

  defp child_spec(_) do
    Logger.info("Webhook provider has been disabled")
    []
  end
end
