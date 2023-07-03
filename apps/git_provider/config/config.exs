import Config
config :git_provider, enabled: false
config :git_provider, pull_interval: "2h"

config :git_provider, :repositories, []

config :git_provider, :github,
  api_uri: "https://api.github.com/graphql",
  api_token: "",
  polling_interval: "1h",
  patterns: []

if File.exists?(Path.expand("#{Mix.env()}.exs", __DIR__)), do: import_config("#{Mix.env()}.exs")
