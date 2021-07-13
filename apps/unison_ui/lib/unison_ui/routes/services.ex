defmodule UnisonUI.Routes.Services do
  @moduledoc false
  require Logger
  use Plug.Router
  alias Common.Service

  plug(:match)
  plug(:dispatch)

  defp services_behaviour, do: Application.fetch_env!(:services, :storage_backend)

  get "/" do
    body =
      services_behaviour().available_services()
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
    case services_behaviour().service(Enum.join(remaining, "/")) do
      {:ok, %Service.Grpc{schema: schema, servers: servers}} ->
        response = %{
          schema: schema,
          servers:
            servers
            |> Enum.into([], fn {name, [address: _, port: _, use_tls: use_tls]} ->
              %{name: name, useTls: use_tls}
            end)
        }

        conn |> put_resp_content_type("text/plain") |> resp(200, Jason.encode!(response))

      {:ok, %Service.OpenApi{content: content}} ->
        conn |> put_resp_content_type("text/plain") |> resp(200, content)

      _ ->
        resp(conn, 404, Plug.Conn.Status.reason_phrase(404))
    end
  end
end
