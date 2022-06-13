import Config

config :k8s,
  clusters: %{
    default: %{}
  }

config :container_provider, enabled: false

config :container_provider, :connection_backoff,
  start: 0,
  interval: 1_000,
  max: 5_000

config :container_provider, :kubernetes,
  enabled: true,
  polling_interval: 1_000

config :container_provider, :docker,
  enabled: true,
  host: "unix:///var/run/docker.sock"

config :container_provider, :labels,
  service_name: "unisonui.service-name",
  openapi: [
    port: "unisonui.openapi.port",
    protocol: "unisonui.openapi.protocol",
    specification_path: "unisonui.openapi.path",
    use_proxy: "unisonui.openapi.use-proxy"
  ],
  asyncapi: [
    port: "unisonui.asyncapi.port",
    protocol: "unisonui.asyncapi.protocol",
    specification_path: "unisonui.asyncapi.path"
  ],
  grpc: [port: "unisonui.grpc.port", tls: "unisonui.grpc.tls"]

if File.exists?(Path.expand("#{Mix.env()}.exs", __DIR__)), do: import_config("#{Mix.env()}.exs")
