defmodule UnisonUI.Services.Realtime.Consumer do
  use GenStage
  require Logger
  alias UnisonUI.SSE.Chunk
  alias Plug.Conn

  @keep_alive_chunk UnisonUI.SSE.Chunk.build(%Chunk{event: "message", data: ""})
  @keep_alive_interval 5 * 1_000

  def start_link(conn), do: GenStage.start_link(__MODULE__, conn)

  def init(conn) do
    keep_alive()
    {:consumer, conn, subscribe_to: [Application.fetch_env!(:services, :aggregator)]}
  end

  def handle_events(events, _from, state) do
    events
    |> Stream.map(&Jason.encode!/1)
    |> Stream.map(&%Chunk{event: "message", data: &1})
    |> Stream.map(&Chunk.build/1)
    |> Stream.map(&send_chunk(state, &1))
    |> Enum.reduce_while({:noreply, [], state}, fn
      {:noreply, [], _} = state, _ -> {:cont, state}
      state, _ -> {:halt, state}
    end)
  end

  def handle_info(:keep_alive, state) do
    keep_alive()
    send_chunk(state, @keep_alive_chunk)
  end

  defp send_chunk(conn, chunk) do
    case Conn.chunk(conn, chunk) do
      {:ok, conn} ->
        {:noreply, [], conn}

      {:error, error} ->
        Logger.warn(to_string(error))
        {:stop, conn}
    end
  end

  defp keep_alive(), do: Process.send_after(self(), :keep_alive, @keep_alive_interval)
end
