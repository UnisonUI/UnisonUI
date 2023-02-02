defmodule WebhookProvider.MixProject do
  use Mix.Project

  def project do
    [
      app: :webhook_provider,
      version: "0.1.0",
      build_path: "../../_build",
      config_path: "../../config/config.exs",
      deps_path: "../../deps",
      lockfile: "../../mix.lock",
      elixir: "~> 1.14",
      start_permanent: Mix.env() == :prod,
      deps: deps()
    ]
  end

  # Run "mix help compile.app" to learn about applications.
  def application do
    [
      extra_applications: [:logger],
      mod: {WebhookProvider.Application, []}
    ]
  end

  # Run "mix help deps" to learn about dependencies.
  defp deps do
    [
      {:services, in_umbrella: true},
      {:u_grpc, in_umbrella: true},
      {:logstash_logger_formatter, "~> 1.1"},
      {:jason, "~> 1.4"},
      {:plug, "~> 1.13"},
      {:plug_cowboy, "~> 2.5"},
      {:cowboy, "~> 2.9", override: true},
      {:cowlib, "~> 2.11", override: true}
    ]
  end
end
