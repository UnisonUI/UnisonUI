defmodule WebhookProvider.Application do
  @moduledoc false

  use Application

  @impl true
  def start(_type, _args) do
    children = [
      {WebhookProvider.SelfSpecificationServer, name: WebhookProvider.SelfSpecificationServer},
      Plug.Cowboy.child_spec(
        scheme: :http,
        plug: WebhookProvider.Routes,
        options: [port: Application.get_env(:webhook_provider, :port, 3000)]
      )
    ]

    opts = [strategy: :one_for_one, name: WebhookProvider.Supervisor]
    Supervisor.start_link(children, opts)
  end
end
