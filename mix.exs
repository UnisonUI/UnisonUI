defmodule Unisonui.MixProject do
  use Mix.Project
  @version "2.0.0"

  defp common_apps,
    do: [
      clustering: :permanent,
      services: :permanent,
      configuration: :permanent,
      u_grpc: :permanent
    ]

  defp providers,
    do: [
      git_provider: :permanent,
      container_provider: :permanent,
      webhook_provider: :permanent
    ]

  defp providers_apps(:all), do: common_apps() ++ providers()
  defp providers_apps(provider), do: [{provider, :permanent} | common_apps()]

  defp web_apps, do: [unison_ui: :permanent]

  defp all_apps, do: common_apps() ++ providers() ++ web_apps()

  defp config_providers,
    do: [
      {Toml.Provider,
       [
         path: {:system, "UNISON_UI_ROOT", "/config.toml"},
         transforms: [Configuration.Transformer]
       ]}
    ]

  defp provider_release(name),
    do: [
      steps: [:assemble],
      applications: providers_apps(name),
      config_providers: config_providers()
    ]

  def project do
    [
      apps_path: "apps",
      version: "2.0.0",
      start_permanent: Mix.env() == :prod,
      deps: deps(),
      test_coverage: [tool: ExCoveralls, summary: [threshold: false]],
      preferred_cli_env: ["coveralls.html": :test, "coveralls.json": :test],
      releases: [
        unisonui_git_provider: provider_release(:git_provider),
        unisonui_container_provider: provider_release(:container_provider),
        unisonui_webhook_provider: provider_release(:webook_provider),
        unisonui: [
          steps: [&npm_deploy/1, :assemble],
          applications: all_apps(),
          config_providers: config_providers()
        ]
      ],
      dialyzer: [
        plt_file: {:no_warn, "priv/plts/dialyzer.plt"},
        flags: [
          "-Wunmatched_returns",
          "-Werror_handling",
          "-Wno_opaque",
          "-Wunderspecs"
        ]
      ]
    ]
  end

  def alias() do
    webapp = ["apps", "unison_ui", "webapp"] |> Path.join() |> Path.expand()

    [
      "npm.install": System.cmd("npm", ["install"], cd: webapp),
      watch:
        [webapp, "node_modules", ".bin", "webpack"]
        |> Path.join()
        |> Path.expand()
        |> System.cmd(["--watch", "--config", "webpack.dev.js", "--progress"], cd: webapp),
      build:
        [webapp, "node_modules", ".bin", "webpack"]
        |> Path.join()
        |> Path.expand()
        |> System.cmd(["--config", "webpack.dev.js", "--progress"], cd: webapp)
    ]
  end

  def npm_deploy(release) do
    env = Mix.env() |> to_string() |> String.downcase()
    webapp = ["apps", "unison_ui", "webapp"] |> Path.join() |> Path.expand()
    System.cmd("npm", ["install"], cd: webapp) |> elem(0) |> Mix.shell().info()
    System.cmd("npm", ["run", "build:#{env}"], cd: webapp) |> elem(0) |> Mix.shell().info()
    release
  end

  defp deps do
    [
      {:toml, "~> 0.6.2"},
      {:version_tasks, "~> 0.12.0", only: [:dev], runtime: false},
      {:credo, "~> 1.6", only: [:dev], runtime: false},
      {:dialyxir, "~> 1.1.0", only: [:dev], runtime: false},
      {:ex_doc, "~> 0.26", only: [:dev], runtime: false},
      {:excoveralls, "~> 0.14", only: [:dev, :test], runtime: false},
      {:inch_ex, "~> 2.1.0-rc.1", only: [:dev, :test], runtime: false}
    ]
  end
end
