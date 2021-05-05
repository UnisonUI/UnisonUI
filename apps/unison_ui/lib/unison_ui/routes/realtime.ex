defmodule UnisonUI.Routes.Realtime do
  @moduledoc false
  require Logger
  use Plug.Router
  plug :match
  plug :dispatch

  get "/" do
    new_conn =
      conn
      |> put_resp_content_type("text/event-stream")
      |> send_chunked(200)

    UnisonUI.Services.Realtime.Consumers.subscribe(new_conn)
  end
end
