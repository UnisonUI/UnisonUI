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
      elixir: "~> 1.14",
      start_permanent: Mix.env() == :prod,
      elixirc_paths: elixirc_paths(Mix.env()),
      deps: deps(),
      test_coverage: [tool: ExCoveralls],
      xref: [exclude: [UnisonUI]]
    ]
  end

  defp elixirc_paths(:test), do: ["lib", "test/support"]
  defp elixirc_paths(_), do: ["lib"]

  def application do
    [
      extra_applications: [:logger, :yamerl, :gen_stage],
      mod: {GitProvider.Application, []}
    ]
  end

  # Run "mix help deps" to learn about dependencies.
  defp deps do
    [
      {:u_grpc, in_umbrella: true},
      {:clustering, in_umbrella: true},
      {:services, in_umbrella: true},
      {:logstash_logger_formatter, "~> 1.1"},
      {:finch, "~> 0.14"},
      {:jason, "~> 1.4"},
      {:horde, "~> 0.8.7"},
      {:type_check, "~> 0.12.4", github: "MaethorNaur/elixir-type_check", branch: "master"},
      {:durex, "~> 0.3"},
      {:ok, "~> 2.3"},
      {:neuron, "~> 5.0"},
      {:yaml_elixir, "~> 2.9"},
      {:mock, "~> 0.3", only: :test}
    ]
  end
end
