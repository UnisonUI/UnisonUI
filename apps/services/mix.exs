defmodule Services.MixProject do
  use Mix.Project

  def project do
    [
      app: :services,
      version: "0.1.0",
      build_path: "../../_build",
      config_path: "../../config/config.exs",
      deps_path: "../../deps",
      lockfile: "../../mix.lock",
      elixir: "~> 1.14",
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
      mod: {Services.Application, []}
    ]
  end

  defp elixirc_paths(:test), do: ["lib", "test/support"]
  defp elixirc_paths(_), do: ["lib"]

  defp deps do
    [
      {:gen_stage, "~> 1.2"},
      {:ok, "~> 2.3"},
      {:type_check, "~> 0.13"},
      {:ra, "~> 2.5"},
      {:logstash_logger_formatter, "~> 1.1"},
      {:jason, "~> 1.4"}
    ]
  end
end
