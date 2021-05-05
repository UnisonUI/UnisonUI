defmodule UnisonUI.Services.Realtime.Consumers do
  use DynamicSupervisor
  alias UnisonUI.Services.Realtime.Consumer
  def start_link, do: DynamicSupervisor.start_link(__MODULE__, nil, name: __MODULE__)

  @impl true
  def init(_args), do: DynamicSupervisor.init(strategy: :one_for_one)

  @spec subscribe(conn :: Plug.Conn.t()) :: no_return()
  def subscribe(conn) do
    {:ok, pid} = DynamicSupervisor.start_child(__MODULE__, {Consumer, conn})
    Process.monitor(pid)

    receive do
      {:DOWN, _reference, :process, ^pid, _type} ->
        nil
    end
  end
end
