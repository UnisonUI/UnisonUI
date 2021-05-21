defmodule GitProvider.MixProject do
  use Mix.Project

  def project do
    [
      app: :git_provider,
      version: "0.1.0",
      build_path: "../../_build",
      config_path: "../../config/config.exs",
      deps_path: "../../deps",
      lockfile: "../../mix.lock",
      elixir: "~> 1.12-rc",
      start_permanent: Mix.env() == :prod,
      deps: deps(),
      test_coverage: [tool: ExCoveralls],
      xref: [exclude: [UnisonUI]]
    ]
  end

  # Run "mix help compile.app" to learn about applications.
  def application do
    [
      extra_applications: [:logger, :yamerl, :gen_stage],
      mod: {GitProvider.Application, []}
    ]
  end

  # Run "mix help deps" to learn about dependencies.
  defp deps do
    [
      {:common, in_umbrella: true},
      {:u_grpc, in_umbrella: true},
      {:services, in_umbrella: true},
      {:logstash_logger_formatter, "~> 1.0"},
      {:gen_stage, "~> 1.0"},
      {:norm, "~> 0.12"},
      {:durex, "~> 0.2"},
      {:ok, "~> 2.3"},
      {:neuron, "~> 5.0"},
      {:yaml_elixir, "~> 2.5"},
      {:mox, "~> 1.0", only: :test},
      {:mock, "~> 0.3.6", only: :test},
      {:finch, "~> 0.7"},
      {:jason, "~> 1.2"}
    ]
  end
end
