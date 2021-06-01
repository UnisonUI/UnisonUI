import Config

config :services, quorum: 1
config :services, interval_node_down_ms: 1_000 * 60 * 60
config :services, :behaviour, Services
config :services, :aggregator, Services.Aggregator

case Mix.env() do
  :test -> import_config "#{Mix.env()}.exs"
  _ -> :ok
end
