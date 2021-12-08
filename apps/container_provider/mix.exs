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
      elixirc_paths: elixirc_paths(Mix.env()),
      test_coverage: [tool: ExCoveralls],
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

  defp elixirc_paths(:test), do: ["lib", "test/support"]
  defp elixirc_paths(_), do: ["lib"]
  # Run "mix help deps" to learn about dependencies.
  defp deps do
    [
      {:u_grpc, in_umbrella: true},
      {:clustering, in_umbrella: true},
      {:services, in_umbrella: true},
      {:logstash_logger_formatter, "~> 1.1"},
      {:horde, "~> 0.8.5"},
      {:norm, "~> 0.13"},
      {:durex, "~> 0.2"},
      {:ok, "~> 2.3"},
      {:mox, "~> 1.0", only: :test},
      {:mock, "~> 0.3.6", only: :test},
      {:bypass, "~> 2.1", only: :test},
      {:plug_cowboy, "~> 2.5", only: :test, override: true},
      {:cowboy, "~> 2.9", only: :test, override: true},
      {:cowlib, "~> 2.11", only: :test, override: true},
      {:mint, "~> 1.4"},
      {:k8s, "~> 1.0"},
      {:jason, "~> 1.2"}
    ]
  end
end
