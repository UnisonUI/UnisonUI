import Config

config :services, :storage_backend, Services.Storage.Memory
config :services, :aggregator, Services.Aggregator
config :services, :raft, quorum: 0

if File.exists?(Path.expand("#{Mix.env()}.exs", __DIR__)), do: import_config("#{Mix.env()}.exs")
