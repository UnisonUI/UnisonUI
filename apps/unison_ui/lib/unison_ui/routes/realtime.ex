defmodule UnisonUI.Routes.Realtime do
  alias UnisonUI.Services.Realtime.Consumers
  @moduledoc false
  @behaviour :cowboy_websocket

  @impl true
  def init(req, state), do: {:cowboy_websocket, req, state}

  @impl true
  def websocket_init(state) do
    _ = Consumers.subscribe(self())
    {:ok, state}
  end

  @impl true
  def websocket_info({:event, event}, state), do: {:reply, {:text, event}, state}

  @impl true
  def websocket_handle(_frame, state), do: {:ok, state}
end
