defmodule Unisonui.MixProject do
  use Mix.Project

  def project do
    [
      apps_path: "apps",
      version: "0.1.0",
      start_permanent: Mix.env() == :prod,
      deps: deps(),
      test_coverage: [tool: ExCoveralls],
      preferred_cli_env: ["coveralls.html": :test,"coveralls.json": :test],
      releases: [
        unison_ui: [
          steps: [&npm_deploy/1, :assemble, :tar],
          applications: [
            u_grpc: :permanent,
            services: :permanent,
            configuration: :permanent,
            git_provider: :permanent,
            unison_ui: :permanent,
            common: :permanent
          ],
          config_providers: [
            {Toml.Provider,
             [
               path: {:system, "UNISON_UI_ROOT", "/config.toml"},
               transforms: [Configuration.LoggingTransformer]
             ]}
          ]
        ]
      ],
      dialyzer: [
        plt_file: {:no_warn, "priv/plts/dialyzer.plt"},
        flags: [
          "-Wunmatched_returns",
          "-Werror_handling",
          "-Wrace_conditions",
          "-Wno_opaque",
          "-Wunderspecs"
        ]
      ]
    ]
  end

  def npm_deploy(release) do
    System.cmd("npm", ["install"], cd: "apps/unison_ui/webapp/")
    System.cmd("npm", ["run", "build:#{Mix.env()}"], cd: "apps/unison_ui/webapp/")
    release
  end

  # Dependencies listed here are available only for this
  # project and cannot be accessed from applications inside
  # the apps folder.
  #
  # Run "mix help deps" for examples and options.
  defp deps do
    [
      {:logstash_logger_formatter, "~> 1.0"},
      {:toml, "~> 0.6"},
      {:gen_stage, "~> 1.0"},
      {:credo, "~> 1.4", only: [:dev], runtime: false},
      {:dialyxir, "~> 1.0.0", only: [:dev], runtime: false},
      {:ex_doc, "~> 0.21", only: [:dev], runtime: false},
      {:excoveralls, "~> 0.13", only: [:dev, :test], runtime: false},
      {:inch_ex, "~> 2.1.0-rc.1", only: [:dev, :test], runtime: false}
    ]
  end
end
