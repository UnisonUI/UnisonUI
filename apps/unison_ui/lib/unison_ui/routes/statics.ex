defmodule UnisonUI.Routes.Statics do
  @moduledoc false
  use Plug.Builder

  plug Plug.Static, from: {:unison_ui, "priv/statics"}, at: "/", gzip: true, brotli: true
  plug :not_found

  def not_found(conn, _) do
    send_resp(conn, 404, "static resource not found")
  end
end
