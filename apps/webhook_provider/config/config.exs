import Config

config :webhook_provider, enabled: false
config :webhook_provider, port: 3000
config :webhook_provider, self_specification: false

if File.exists?(Path.expand("#{Mix.env()}.exs", __DIR__)), do: import_config("#{Mix.env()}.exs")
