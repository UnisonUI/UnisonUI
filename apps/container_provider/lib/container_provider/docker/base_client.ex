defmodule ContainerProvider.Docker.BaseClient do
  @callback request(path :: String.t(), from :: pid() | nil) ::
              :ok | {:ok, map()} | {:error, term()}
  @callback reconnect() :: :ok | {:error, term()}

  defmacro __using__(_opts) do
    quote do
      @behaviour ContainerProvider.Docker.BaseClient
      use GenServer
      require Logger
      @headers [{"content-type", "application/json"}]
      defstruct [:conn, :uri, requests: %{}]

      def start_link(uri), do: GenServer.start_link(__MODULE__, uri, name: __MODULE__)

      @spec request(path :: String.t(), from :: pid() | nil) ::
              :ok | {:ok, map()} | {:error, term()}
      def request(path, from \\ nil),
        do: GenServer.call(__MODULE__, {:request, URI.encode(path), from})

      @spec reconnect() :: :ok | {:error, term()}
      def reconnect(), do: GenServer.call(__MODULE__, :reconnect)

      @impl true
      def init(uri) do
        case connect(uri) do
          {:ok, conn} ->
            state = %__MODULE__{conn: conn, uri: uri}
            {:ok, state}

          {:error, reason} ->
            {:stop, reason}
        end
      end

      defp connect(uri) when is_binary(uri), do: uri |> parse_uri() |> connect()

      defp connect({scheme, host, port, options}),
        do: Mint.HTTP.connect(scheme, host, port, options)

      defp parse_uri(uri) when is_binary(uri), do: uri |> URI.parse() |> parse_uri()

      defp parse_uri(%URI{scheme: "unix", path: path}),
        do: {:http, {:local, path}, 0, [hostname: "localhost"]}

      defp parse_uri(%URI{scheme: scheme, port: port, host: host}),
        do: {String.to_atom(scheme), host, port, []}

      @impl true
      def handle_call({:request, path, pid}, from, state) do
        from = pid || from
        streaming = !is_nil(pid)

        case Mint.HTTP.request(state.conn, "GET", path, @headers, nil) do
          {:ok, conn, request_ref} ->
            state = put_in(state.conn, conn)

            state =
              put_in(state.requests[request_ref], %{
                from: from,
                response: %{},
                streaming: streaming
              })

            if streaming, do: {:reply, :ok, state}, else: {:noreply, state}

          {:error, conn, reason} ->
            state = put_in(state.conn, conn)
            {:reply, {:error, reason}, state}
        end
      end

      def handle_call(:reconnect, _from, state) do
        case connect(state.uri) do
          {:ok, conn} ->
            {:reply, :ok, %__MODULE__{conn: conn, uri: state.uri}}

          error ->
            {:reply, error, put_in(state.requests, %{})}
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

          {:error, conn, reason, _} ->
            state.requests
            |> Enum.each(fn
              {_, %{from: from, streaming: false}} -> GenServer.reply(from, {:error, reason})
              {_, %{from: pid, streaming: true}} -> reply_sender(pid, {:error, reason})
            end)

            state = put_in(state.conn, conn)
            {:noreply, put_in(state.requests, %{})}
        end
      end

      defp process_response({:status, request_ref, status}, state) do
        state = put_in(state.requests[request_ref].response[:status], status)

        if state.requests[request_ref].streaming do
          reply_sender(state.requests[request_ref].from, {:status, status})
        end

        state
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
          new_data =
            case Jason.decode(new_data) do
              {:ok, data} -> data
              _ -> new_data
            end

          tag = if state.requests[request_ref].response[:status] >= 400, do: :error, else: :data
          reply_sender(state.requests[request_ref].from, {tag, new_data})
          state
        end
      end

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

      defp process_response({:error, request_ref, reason}, state) do
        {%{from: from, streaming: streaming}, state} = pop_in(state.requests[request_ref])

        unless streaming do
          GenServer.reply(from, {:error, reason})
        else
          reply_sender(from, {:error, reason})
        end

        state
      end

      defp socket_error?(message), do: String.ends_with?(message, "connection refused \n")
      defp reply_sender({from, _}, response), do: reply_sender(from, response)
      defp reply_sender(from, response), do: send(from, {:stream, response})
    end
  end
end
