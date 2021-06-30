defmodule ContainerProvider.MixProject do
  use Mix.Project

  def project do
    [
      app: :container_provider,
      version: "2.0.0",
      build_path: "../../_build",
      config_path: "../../config/config.exs",
      deps_path: "../../deps",
      lockfile: "../../mix.lock",
      elixir: "~> 1.12",
      start_permanent: Mix.env() == :prod,
      deps: deps()
    ]
  end

  # Run "mix help compile.app" to learn about applications.
  def application do
    [
      extra_applications: [:logger],
      mod: {ContainerProvider.Application, []}
    ]
  end

  # Run "mix help deps" to learn about dependencies.
  defp deps do
    [
      {:common, in_umbrella: true},
      {:u_grpc, in_umbrella: true},
      {:clustering, in_umbrella: true},
      {:services, in_umbrella: true},
      {:logstash_logger_formatter, "~> 1.0"},
      {:horde, "~> 0.8"},
      {:norm, "~> 0.12"},
      {:durex, "~> 0.2"},
      {:ok, "~> 2.3"},
      {:mox, "~> 1.0", only: :test},
      {:mock, "~> 0.3.6", only: :test},
      {:mint, "~> 1.3"},
      {:k8s, "~> 0.5"},
      {:jason, "~> 1.2"}
    ]
  end
end
