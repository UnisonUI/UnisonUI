import Config

config :database, quorum: 1

config :database,
  stores: [
    Database.Schema.Migration,
    Database.Schema.Events,
    Database.Schema.State
  ]
