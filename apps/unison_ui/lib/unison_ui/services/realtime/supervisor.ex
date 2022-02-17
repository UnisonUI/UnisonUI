defmodule UnisonUI.Services.Realtime.Supervisor do
  use Supervisor
  @spec start_link() :: Supervisor.on_start()
  def start_link, do: Supervisor.start_link(__MODULE__, :ok, name: __MODULE__)

  def init(:ok), do: Supervisor.init(child_spec(), strategy: :one_for_one)

  defp child_spec,
    do: [
      %{
        id: UnisonUI.Services.Realtime.Consumers,
        start: {UnisonUI.Services.Realtime.Consumers, :start_link, []}
      }
    ]
end
