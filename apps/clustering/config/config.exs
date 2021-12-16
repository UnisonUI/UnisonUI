import Config

config :clustering, provider: false

config :clustering, hosts: []

config :clustering, :kubernetes, service: "unisonui"

config :clustering, :aws,
  tag_name: "app",
  tag_value: "unisonui"
