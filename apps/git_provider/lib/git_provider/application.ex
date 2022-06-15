defmodule GitProvider.Application do
  @moduledoc false
  require Logger
  use Application

  def start(_type, _args) do
    opts = [strategy: :one_for_one, name: GitProvider.Supervisor]

    Application.fetch_env!(:git_provider, :enabled)
    |> child_spec()
    |> Supervisor.start_link(opts)
  end

  defp child_spec(false) do
    Logger.info("Git provider has been disabled")
    []
  end

  defp child_spec(_),
    do: [
      %{id: GitProvider.Git.Supervisor, start: {GitProvider.Git.Supervisor, :start_link, []}},
      {Finch, name: NeuroFinch},
      %{
        id: GitProvider.Github.Supervisor,
        start: {GitProvider.Github.Supervisor, :start_link, []}
      },
      %{
        id: GitProvider.Gitlab.Supervisor,
        start: {GitProvider.Gitlab.Supervisor, :start_link, []}
      }
    ]
end
