defmodule ContainerProvider.Docker.Client do
  use GenServer
  require Logger
  @headers [{"content-type", "application/json"}]
  defstruct [:conn, :conn_param, requests: %{}]

  def start_link(uri), do: GenServer.start_link(__MODULE__, uri)

  def get(pid, path), do: GenServer.call(pid, {:get, path})

  def events(pid, query, from),
    do: GenServer.cast(pid, {:get, "/events?" <> URI.encode(query), from})

  @impl true
  def init(uri) do
    {scheme, host, port, options} = parse_uri(uri)

    case Mint.HTTP.connect(scheme, host, port, options) do
      {:ok, conn} ->
        state = %__MODULE__{conn: conn, conn_param: {scheme, host, port, options}}
        {:ok, state}

      {:error, reason} ->
        {:stop, reason}
    end
  end

  defp parse_uri(uri) when is_binary(uri), do: uri |> URI.parse() |> parse_uri()

  defp parse_uri(%URI{scheme: "unix", path: path}),
    do: {:http, {:local, path}, 0, [hostname: "localhost"]}

  defp parse_uri(%URI{scheme: scheme, port: port, host: host}),
    do: {String.to_atom(scheme), host, port, []}

  @impl true
  def handle_call({:get, path}, from, state) do
    case Mint.HTTP.request(state.conn, "GET", path, @headers, nil) do
      {:ok, conn, request_ref} ->
        state = put_in(state.conn, conn)

        state =
          put_in(state.requests[request_ref], %{from: from, response: %{}, streaming: false})

        {:noreply, state}

      {:error, conn, %Mint.HTTPError{reason: :closed} = reason} ->
        state = put_in(state.requests, %{})
        state = put_in(state.conn, conn)

        case reconnect(state.conn_param) do
          {:ok, conn} ->
            {:reply, {:error, reason}, put_in(state.conn, conn)}

          {:error, reason} ->
            {:reply, {:error, reason}, state}
        end

      {:error, conn, reason} ->
        state = put_in(state.conn, conn)
        {:reply, {:error, reason}, state}
    end
  end

  def reconnect({scheme, host, port, options}), do: Mint.HTTP.connect(scheme, host, port, options)

  @impl true
  def handle_cast({:get, query, pid}, state) do
    case Mint.HTTP.request(state.conn, "GET", "/events?" <> query, @headers, nil) do
      {:ok, conn, request_ref} ->
        state = put_in(state.conn, conn)
        state = put_in(state.requests[request_ref], %{from: pid, response: %{}, streaming: true})
        {:noreply, state}

      {:error, conn, %Mint.HTTPError{reason: :closed} = reason} ->
        state = put_in(state.requests, %{})
        state = put_in(state.conn, conn)

        case reconnect(state.conn_param) do
          {:ok, conn} ->
            send(pid, {:error, reason})

            {:noreply, put_in(state.conn, conn)}

          {:error, reason} ->
            send(pid, {:error, reason})
            {:noreply, state}
        end

      {:error, conn, reason} ->
        state = put_in(state.conn, conn)
        send(pid, {:error, reason})
        {:noreply, state}
    end
  end

  @impl true
  def handle_info(message, state) do
    case Mint.HTTP.stream(state.conn, message) do
      :unknown ->
        _ = Logger.debug(fn -> "Received unknown message: " <> inspect(message) end)
        {:noreply, state}

      {:ok, conn, responses} ->
        state = put_in(state.conn, conn)
        state = Enum.reduce(responses, state, &process_response/2)
        {:noreply, state}

      {:error, reason} ->
        state.requests
        |> Enum.each(fn
          %{from: from, streaming: false} -> GenServer.reply(from, {:error, reason})
          %{from: pid, streaming: true} -> reply_sender(pid, {:error, reason})
        end)

        {:noreply, put_in(state.requests, %{})}
    end
  end

  defp process_response({:status, request_ref, status}, state) do
    unless state.requests[request_ref].streaming do
      put_in(state.requests[request_ref].response[:status], status)
    else
      reply_sender(state.requests[request_ref].from, {:status, status})
      state
    end
  end

  defp process_response({:headers, request_ref, headers}, state) do
    unless state.requests[request_ref].streaming do
      put_in(state.requests[request_ref].response[:headers], headers)
    else
      reply_sender(state.requests[request_ref].from, {:headers, headers})
      state
    end
  end

  defp process_response({:data, request_ref, new_data}, state) do
    unless state.requests[request_ref].streaming do
      update_in(state.requests[request_ref].response[:data], fn data ->
        (data || "") <> new_data
      end)
    else
      reply_sender(state.requests[request_ref].from, {:data, Jason.decode!(new_data)})
      state
    end
  end

  # When the request is done, we use GenServer.reply/2 to reply to the caller that was
  # blocked waiting on this request.
  defp process_response({:done, request_ref}, state) do
    {%{response: response, from: from, streaming: streaming}, state} =
      pop_in(state.requests[request_ref])

    unless streaming do
      response =
        update_in(response[:data], fn data ->
          Jason.decode!(data || "")
        end)

      GenServer.reply(from, {:ok, response})
    else
      reply_sender(from, :done)
    end

    state
  end

  defp reply_sender({from, _}, response), do: reply_sender(from, response)
  defp reply_sender(from, response), do: send(from, {:stream, response})
end
