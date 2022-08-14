import Config

config :services, :storage_backend, Services.Storage.Memory
config :services, :aggregator, Services.Aggregator
config :services, :raft, quorum: 0
config :services, :sql, driver: :postgres, url: "ecto://postgres:postgres@localhost/ecto_simple"

config :services, Services.Storage.Sql.Postgres.Repo, []
config :services, Services.Storage.Sql.Mysql.Repo, []

config :services, :ecto_repos, [
  Services.Storage.Sql.Postgres.Repo,
  Services.Storage.Sql.Mysql.Repo
]

if File.exists?(Path.expand("#{Mix.env()}.exs", __DIR__)), do: import_config("#{Mix.env()}.exs")
