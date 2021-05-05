defmodule UnisonUI.Services.Supervisor do
  use Elixir.Supervisor
  @spec start_link() :: {:ok, pid} | {:error, {:already_started, pid} | {:shutdown, term} | term}
  def start_link, do: Elixir.Supervisor.start_link(__MODULE__, :ok, name: __MODULE__)

  def init(:ok), do: child_spec() |> Elixir.Supervisor.init(strategy: :one_for_one)

  defp child_spec,
    do: [
      %{
        id: UnisonUI.Services.Realtime.Supervisor,
        start: {UnisonUI.Services.Realtime.Supervisor, :start_link, []}
      }
    ]
end
