import Config
config :git_provider, enabled: true
config :git_provider, pull_interval: "2h"

config :git_provider, :repositories, []

config :git_provider, :github,
  api_uri: "https://api.github.com/graphql",
  api_token: "",
  polling_interval: "1h",
  patterns: []
