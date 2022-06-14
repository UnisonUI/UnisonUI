defmodule GRPC.MixProject do
  use Mix.Project

  def project do
    [
      app: :u_grpc,
      version: "0.1.0",
      build_path: "../../_build",
      config_path: "../../config/config.exs",
      deps_path: "../../deps",
      lockfile: "../../mix.lock",
      elixir: "~> 1.13",
      elixirc_paths: elixirc_paths(Mix.env()),
      start_permanent: Mix.env() == :prod,
      test_coverage: [tool: ExCoveralls],
      deps: deps()
    ]
  end

  # Run "mix help compile.app" to learn about applications.
  def application do
    [
      extra_applications: [:logger],
      mod: {GRPC.Application, []}
    ]
  end

  defp elixirc_paths(:test), do: ["lib", "test/support"]
  defp elixirc_paths(_), do: ["lib"]

  # Run "mix help deps" to learn about dependencies.
  defp deps do
    [
      {:jason, "~> 1.3"},
      {:castore, "~> 0.1.15"},
      {:protox, "~> 1.6.10"},
      {:mint, "~> 1.4", override: true},
      {:grpc, "~> 0.5.0-beta.1", only: :test},
      {:cowboy, "~> 2.9", only: :test, override: true},
      {:cowlib, "~> 2.11", only: :test, override: true},
      {:protobuf, "== 0.9.0", only: :test},
      {:ok, "~> 2.3"},
      {:gen_state_machine, "~> 3.0"},
      {:logstash_logger_formatter, "~> 1.1"}
    ]
  end
end
