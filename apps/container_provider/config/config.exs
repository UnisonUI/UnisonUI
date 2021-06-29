import Config
config :container_provider, enabled: true

config :container_provider, :connection_backoff,
  start: 0,
  interval: 1_000,
  max: 5_000

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
  grpc: [port: "unisonui.grpc.port", tls: "unisonui.grpc.tls"]
