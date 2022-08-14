defmodule Services.Storage.Sql.Supervisor do
  use Supervisor

  @spec start_link(term()) :: Supervisor.on_start()
  def start_link(opts), do: Supervisor.start_link(__MODULE__, opts, name: __MODULE__)

  @impl true
  def init(_) do
    config = Application.fetch_env!(:services, :sql)
    driver = config[:driver] |> to_string() |> String.capitalize()
    repo = :"Services.Storage.Sql.Events.#{driver}.Repo "
    Supervisor.init([repo], strategy: :one_for_one)
  end
end
