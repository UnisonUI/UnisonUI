import Config

config :unison_ui, self_specification: false
config :unison_ui, port: 8080

if File.exists?(Path.expand("#{Mix.env()}.exs", __DIR__)), do: import_config("#{Mix.env()}.exs")
