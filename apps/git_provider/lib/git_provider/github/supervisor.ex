defmodule GitProvider.Github.Supervisor do
  @graphql_uri "https://api.github.com/graphql"
  use Elixir.Supervisor
  alias GitProvider.GraphQL.Settings

  @spec start_link() :: Elixir.Supervisor.on_start()
  def start_link, do: Elixir.Supervisor.start_link(__MODULE__, :ok, name: __MODULE__)

  @impl true
  def init(:ok) do
    Settings.from_env(:github, @graphql_uri)
    |> child_spec()
    |> Supervisor.init(strategy: :one_for_one)
  end

  defp child_spec(%{api_token: api_token} = settings) when api_token != "",
    do: [
      %{
        id: {GitProvider.GraphQL, :github},
        start: {GitProvider.GraphQL, :start_link, [{GitProvider.Github.Client, settings}]}
      }
    ]

  defp child_spec(_settings), do: []
end
