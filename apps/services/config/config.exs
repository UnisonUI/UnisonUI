import Config

config :services, quorum: 1
config :services, :behaviour, Services
config :services, :aggregator, Services.Aggregator

case Mix.env() do
  :test -> import_config "#{Mix.env()}.exs"
  _ -> :ok
end
