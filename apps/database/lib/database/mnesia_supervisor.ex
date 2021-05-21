defmodule Database.MnesiaSupervisor do
  @moduledoc false
  use Elixir.Supervisor
  def start_link, do: Elixir.Supervisor.start_link(__MODULE__, :ok, name: __MODULE__)

  def init(:ok) do
    children = [
      %{
        id: Database.Mnesia.Cluster,
        start: {Database.Mnesia.Cluster, :start_link, []}
      }
    ]

    Elixir.Supervisor.init(children, strategy: :one_for_one)
  end
end
