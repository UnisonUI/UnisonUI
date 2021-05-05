import Config
# config :git_provider, enabled: false
config :git_provider, pull_interval: "15s"

config :git_provider, :git, []
config :logger, truncate: :infinity
config :git_provider, :github,
  api_uri: "https://api.github.com/graphql",
  api_token: "1ea1ce57fe2f2d2f523bd62954cfc96d7f802092",
  polling_interval: "15s",
  repositories: ["MaethorNaur/restui-.+"]
