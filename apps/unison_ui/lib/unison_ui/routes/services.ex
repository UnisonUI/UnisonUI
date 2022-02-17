defmodule UnisonUI.Routes.Services do
  @moduledoc false
  require Logger
  use Plug.Router

  plug :match
  plug :dispatch

  get "/" do
    body =
      Services.available_services()
      |> then(fn
        {:ok, services} ->
          services

        {:error, error} ->
          Logger.warn(inspect(error))
          []
      end)
      |> Jason.encode!()

    resp(conn, 200, body)
  end

  get "/*remaining" do
    case Services.service(Enum.join(remaining, "/")) do
      {:ok, %Services.Grpc{schema: schema, servers: servers}} ->
        response = %{
          schema: schema,
          servers:
            Enum.into(servers, [], fn {name, server} ->
              %{name: name, useTls: server[:use_tls]}
            end)
        }

        conn |> put_resp_content_type("application/json") |> resp(200, Jason.encode!(response))

      {:ok, %{content: content}} ->
        conn |> put_resp_content_type("text/plain") |> resp(200, content)

      _ ->
        resp(conn, 404, Plug.Conn.Status.reason_phrase(404))
    end
  end
end
