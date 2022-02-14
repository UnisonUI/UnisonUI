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
      {:finch, "~> 0.9.1"},
      {:jason, "~> 1.3"},
      {:horde, "~> 0.8.6"},
      {:norm, "~> 0.13"},
      {:durex, "~> 0.2"},
      {:ok, "~> 2.3"},
      {:neuron, "~> 5.0"},
      {:yaml_elixir, "~> 2.5"},
      {:mock, "~> 0.3.6", only: :test},
      {:stream_data, "~> 0.5", only: [:test, :dev]}
    ]
  end
end
