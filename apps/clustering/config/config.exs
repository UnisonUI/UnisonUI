import Config

config :clustering, provider: false

config :clustering, hosts: []

config :clustering, :kubernetes, service: "unisonui", application_name: "unison_ui"
config :clustering, gcp: "unison_ui"

config :clustering, :aws,
  tag_name: "app",
  tag_value: "unisonui",
  app_prefix: "unison_ui"
