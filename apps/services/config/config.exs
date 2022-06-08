import Config

config :services, :storage_backend, Services.Storage.Memory
config :services, :aggregator, Services.Aggregator
config :services, :raft, nodes: [], quorum: 1
