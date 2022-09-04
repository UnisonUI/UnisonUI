defmodule Configuration.MixProject do
  use Mix.Project

  def project do
    [
      app: :configuration,
      version: "0.1.0",
      build_path: "../../_build",
      config_path: "../../config/config.exs",
      deps_path: "../../deps",
      lockfile: "../../mix.lock",
      elixir: "~> 1.14",
      test_coverage: [tool: ExCoveralls],
      start_permanent: Mix.env() == :prod,
      deps: deps()
    ]
  end

  # Run "mix help compile.app" to learn about applications.
  def application do
    [
      extra_applications: [:logger]
    ]
  end

  # Run "mix help deps" to learn about dependencies.
  defp deps do
    [
      {:toml, "~> 0.6"},
      {:logstash_logger_formatter, "~> 1.1"}
    ]
  end
end
