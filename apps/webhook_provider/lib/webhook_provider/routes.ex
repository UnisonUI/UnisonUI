defmodule WebhookProvider.Routes do
  @moduledoc false
  require Logger
  use Plug.Router
  use Plug.ErrorHandler

  if Mix.env() == :dev do
    use Plug.Debugger
  end

  plug Plug.Logger, log: :debug
  plug :match
  plug :dispatch
  forward "/webhook/services", to: WebhookProvider.Routes.Services

  match _ do
    send_resp(conn, 404, "not found")
  end
end
