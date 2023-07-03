defmodule UnisonUI.Routes.Realtime do
  alias UnisonUI.Services.Realtime.Consumers
  @moduledoc false
  @behaviour :cowboy_websocket

  @impl true
  def init(req, state), do: {:cowboy_websocket, req, state}

  @impl true
  def websocket_init(state) do
    _ = Consumers.subscribe(self())

    case Services.available_services() do
      {:ok, services} ->
        events = Enum.into(services, [], &%Services.Event.Up{service: &1})
        {:reply, {:text, Jason.encode!(%{events: events})}, state}

      _ ->
        {:stop, state}
    end
  end

  @impl true
  def websocket_info({:event, event}, state), do: {:reply, {:text, event}, state}

  @impl true
  def websocket_handle(:ping, state), do: {:reply, :pong, state}

  @impl true
  def websocket_handle({:text, "ping"}, state), do: {:reply, :pong, state}

  @impl true
  def websocket_handle(_frame, state), do: {:ok, state}
end
