defmodule ConsulProvider.MixProject do
  use Mix.Project

  def project do
    [
      app: :consul_provider,
      version: "0.1.0",
      build_path: "../../_build",
      config_path: "../../config/config.exs",
      deps_path: "../../deps",
      lockfile: "../../mix.lock",
      elixir: "~> 1.13",
      start_permanent: Mix.env() == :prod,
      test_coverage: [tool: ExCoveralls],
      deps: deps()
    ]
  end

  # Run "mix help compile.app" to learn about applications.
  def application do
    [
      extra_applications: [:logger],
      mod: {ConsulProvider.Application, []}
    ]
  end

  # Run "mix help deps" to learn about dependencies.
  defp deps do
    [
      {:finch, "~> 0.13.0"},
      {:ok, "~> 2.3"},
      {:type_check, "~> 0.12.1", github: "MaethorNaur/elixir-type_check", branch: "master"},
      {:jason, "~> 1.3"}
    ]
  end
end
