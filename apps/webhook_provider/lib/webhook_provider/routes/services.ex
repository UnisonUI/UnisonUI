defmodule WebhookProvider.Routes.Services do
  @moduledoc false
  use Plug.Router
  alias WebhookProvider.Service
  alias Services.Event
  plug :match

  plug Plug.Parsers,
    parsers: [:json],
    pass: ["application/json"],
    json_decoder: Jason

  plug :dispatch

  post "/" do
    case Service.from_map(conn.body_params) do
      nil ->
        resp(conn, 400, "Invalid input")

      service ->
        _ = Services.dispatch_events([%Event.Up{service: service}])
        resp(conn, 204, "")
    end
  end

  delete "/:service_name" do
    _ = Services.dispatch_events([%Event.Down{id: Service.id(service_name)}])
    resp(conn, 204, "")
  end

  match _ do
    send_resp(conn, 404, "not found")
  end
end
