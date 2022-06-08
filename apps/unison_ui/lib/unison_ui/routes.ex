defmodule UnisonUI.Routes do
  @moduledoc false
  require Logger
  use Plug.Router
  use Plug.ErrorHandler

  if Mix.env() == :dev do
    use Plug.Debugger
  end

  plug Plug.Logger, log: :debug
  plug :redirect_index
  plug :match
  plug :dispatch

  forward "/statics", to: UnisonUI.Routes.Statics

  match _ do
    send_resp(conn, 404, "not found")
  end

  def redirect_index(%Plug.Conn{path_info: path} = conn, _opts) do
    case path do
      [] ->
        %{conn | path_info: ["statics", "index.html"]}

      ["service" | _] ->
        %{conn | path_info: ["statics", "index.html"]}

      ["favicon.ico"] ->
        %{conn | path_info: ["statics", "favicon.ico"]}

      _ ->
        conn
    end
  end
end
