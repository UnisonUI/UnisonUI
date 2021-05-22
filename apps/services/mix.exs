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
      mod: {Services.Application, []}
    ]
  end

  # Run "mix help deps" to learn about dependencies.
  defp deps do
    [
      {:common, in_umbrella: true},
      {:gen_stage, "~> 1.0"},
      {:ok, "~> 2.3"},
      {:gen_state_machine, "~> 3.0"},
      {:ra, "~> 1.1"},
      {:ex_unit_clustered_case, "~> 0.4.0", only: :test},
      {:logstash_logger_formatter, "~> 1.0"}
    ]
  end
end
