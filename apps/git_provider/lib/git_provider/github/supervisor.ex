defmodule GitProvider.Github.Supervisor do
  use Elixir.Supervisor
  alias GitProvider.Github.Settings

  @spec start_link() :: Elixir.Supervisor.on_start()
  def start_link, do: Elixir.Supervisor.start_link(__MODULE__, :ok, name: __MODULE__)

  @impl true
  def init(:ok) do
    settings = Application.fetch_env!(:git_provider, :github)

    struct(Settings, settings)
    |> Map.update!(:polling_interval, fn
      value when is_integer(value) -> value
      value -> Durex.ms!(value)
    end)
    |> child_spec()
    |> Supervisor.init(strategy: :one_for_one)
  end

  defp child_spec(%{api_token: api_token} = settings) when api_token != "",
    do: [
      {Finch, name: NeuroFinch},
      %{
        id: GitProvider.Github,
        start: {GitProvider.Github, :start_link, [settings]}
      }
    ]

  defp child_spec(_settings), do: []
end
