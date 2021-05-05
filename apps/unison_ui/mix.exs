defmodule UnisonUI.MixProject do
  use Mix.Project

  def project do
    [
      app: :unison_ui,
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
      mod: {UnisonUI.Application, []}
    ]
  end

  # Run "mix help deps" to learn about dependencies.
  defp deps do
    [
      {:common, in_umbrella: true},
      {:services, in_umbrella: true},
      {:logstash_logger_formatter, "~> 1.0"},
      {:gen_stage, "~> 1.0"},
      {:jason, "~> 1.2"},
      {:plug, "~> 1.11"},
      {:plug_cowboy, "~> 2.4"}
    ]
  end
end
