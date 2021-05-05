import Config

config :unison_ui, port: 8080

case Mix.env() do
  :test -> import_config "#{Mix.env()}.exs"
  _ -> :ok
end
