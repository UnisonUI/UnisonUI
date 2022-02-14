defmodule Clustering.MixProject do
  use Mix.Project

  def project do
    [
      app: :clustering,
      version: "0.1.0",
      build_path: "../../_build",
      config_path: "../../config/config.exs",
      deps_path: "../../deps",
      lockfile: "../../mix.lock",
      elixir: "~> 1.10",
      start_permanent: Mix.env() == :prod,
      deps: deps()
    ]
  end

  # Run "mix help compile.app" to learn about applications.
  def application do
    [
      extra_applications: [:logger],
      mod: {Clustering.Application, []}
    ]
  end

  # Run "mix help deps" to learn about dependencies.
  defp deps do
    [
      {:libcluster, "~> 3.3.1"},
      {:horde, "~> 0.8.6"},
      {:libcluster_gce_strategy, "~> 0.1", only: :prod},
      {:libcluster_ec2, "~> 0.1",
       only: :prod, github: "UnisonUI/libcluster_ec2", branch: "master"}
    ]
  end
end
