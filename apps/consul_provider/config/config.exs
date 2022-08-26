import Config

config :consul_provider, enabled: false
config :consul_provider, token: ""
config :consul_provider, base_url: "http://localhost:8500"

config :consul_provider, :labels,
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
