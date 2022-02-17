defmodule UnisonUI.Services.Realtime.Consumers do
  use DynamicSupervisor
  alias UnisonUI.Services.Realtime.Consumer
  require Logger
  def start_link, do: DynamicSupervisor.start_link(__MODULE__, nil, name: __MODULE__)

  @impl true
  def init(_args), do: DynamicSupervisor.init(strategy: :one_for_one)

  @spec subscribe(pid :: pid()) :: :ok
  def subscribe(pid) do
    {:ok, _} = DynamicSupervisor.start_child(__MODULE__, {Consumer, pid})
    :ok
  end
end
