defmodule Grpc.MixProject do
  use Mix.Project

  def project do
    [
      app: :grpc,
      version: "0.1.0",
      build_path: "../../_build",
      config_path: "../../config/config.exs",
      deps_path: "../../deps",
      lockfile: "../../mix.lock",
      elixir: "~> 1.12-rc",
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

  # Run "mix help deps" to learn about dependencies.
  defp deps do
    [
      {:jason, "~> 1.2"},
      {:castore, "~> 0.1.9"},
      {:protox, "~> 1.3"},
      {:mint, "~> 1.3", override: true},
      {:ex_grpc, "~> 0.5.0-beta.1", hex: :grpc, only: :test},
      {:ok, "~> 2.3"},
      {:gen_state_machine, "~> 3.0"},
      {:logstash_logger_formatter, "~> 1.0"}
    ]
  end
end
