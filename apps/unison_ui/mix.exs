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
      {:services, in_umbrella: true},
      {:logstash_logger_formatter, "~> 1.1"},
      {:jason, "~> 1.2"},
      {:plug, "~> 1.12.1"},
      {:plug_cowboy, "~> 2.5.2"},
      {:cowboy, "~> 2.9", override: true},
      {:cowlib, "~> 2.11", override: true}
    ]
  end
end
