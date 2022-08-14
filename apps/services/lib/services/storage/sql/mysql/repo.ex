defmodule Services.Storage.Sql.Mysql.Repo do
  use Ecto.Repo,
    otp_app: :services,
    adapter: Ecto.Adapters.MyXQL

  @spec init(any, keyword) :: {:ok, [{atom, any}, ...]}
  def init(_type, config) do
    url =
      Application.fetch_env!(:services, :sql) |> Keyword.get(:url, System.get_env("DATABASE_URL"))

    {:ok, Keyword.put(config, :url, url)}
  end
end
