import Config

config :logger, :console,
  level: :debug,
  format: "$date $time [$level] $metadata$message\n",
  metadata: [:application, :mfa, :registered_name]

for config <- "../apps/*/config/config.exs" |> Path.expand(__DIR__) |> Path.wildcard() do
  import_config config
end

case Mix.env() do
  :prod -> import_config "#{Mix.env()}.exs"
  _ -> :ok
end
