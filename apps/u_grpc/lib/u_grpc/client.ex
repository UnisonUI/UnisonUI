defmodule UGRPC.Client do
  defmodule Connection do
    defstruct [:pid, :ref]
  end

  @headers [
    {"content-type", "application/grpc"},
    {"user-agent", "grpc-unisonui/1.0.0"},
    {"te", "trailers"}
  ]

  require Logger
  use GenServer, restart: :temporary

  alias UGRPC.Error
  alias UGRPC.Client.{GzipCompressor, Connection}
  alias UGRPC.Protobuf.Structs.Schema
  alias UGRPC.Protobuf.Serde
  alias Mint.HTTP2

  defstruct [:conn, requests: %{}]

  @spec start_link(server :: String.t()) :: GenServer.on_start()
  def start_link(server), do: GenServer.start_link(__MODULE__, server)

  @impl true
  def init(server) do
    case connect(server) do
      {:ok, conn} -> {:ok, %__MODULE__{conn: conn}}
      {:error, reason} -> {:stop, reason}
    end
  end

  defp connect(server) do
    uri = URI.parse(server)
    HTTP2.connect(String.to_atom(uri.scheme), uri.host, uri.port)
  end

  @spec request(
          stream :: UGRPC.Client.Connection.t(),
          schema :: Protobuf.Structs.Schema.t(),
          service_name :: String.t(),
          method_name :: String.t()
        ) ::
          {:ok, UGRPC.Client.Connection.t()} | {:error, term()}
  def request(%Connection{pid: pid} = stream, schema, service_name, method_name),
    do: GenServer.call(pid, {:request, stream, schema, service_name, method_name})

  @spec send_data(stream :: UGRPC.Client.Connection.t(), data :: map()) :: :ok
  def send_data(%Connection{pid: pid, ref: request_ref}, data),
    do: GenServer.cast(pid, {:send, request_ref, data})

  @spec close(stream :: UGRPC.Client.Connection.t()) :: :ok
  def close(%Connection{pid: pid, ref: request_ref}),
    do: GenServer.cast(pid, {:send, request_ref, :eof})

  @impl true
  def handle_call(
        {:request, stream, %Schema{services: services} = schema, service_name, method_name},
        from,
        state
      ) do
    methods = services |> Map.get(service_name, %{}) |> Map.get(:methods, [])

    path = "/#{service_name}/#{method_name}"

    case Enum.find(methods, &match?(%{name: ^method_name}, &1)) do
      nil ->
        {:reply, {:error, :not_found}, state}

      %{
        server_streaming?: server_streaming?,
        client_streaming?: client_streaming?,
        input_type: input_type,
        output_type: output_type
      } ->
        mode =
          case {server_streaming?, client_streaming?} do
            {false, false} -> :unary
            _ -> :streaming
          end

        state = do_request(stream, mode, path, input_type, output_type, schema, from, state)
        {:noreply, state}
    end
  end

  defp do_request(stream, mode, path, input_type, output_type, schema, from, state) do
    headers =
      if GzipCompressor.enabled?() do
        [
          {"grpc-encoding", GzipCompressor.name()},
          {"grpc-accept-encoding", GzipCompressor.name()} | @headers
        ]
      else
        @headers
      end

    case Mint.HTTP2.request(state.conn, "POST", path, headers, :stream) do
      {:ok, conn, request_ref} ->
        state = put_in(state.conn, conn)

        state =
          put_in(state.requests[request_ref], %{
            schema: schema,
            from: from,
            input_type: input_type,
            output_type: output_type,
            mode: mode
          })

        GenServer.reply(from, {:ok, put_in(stream.ref, request_ref)})
        state

      {:error, conn, %Mint.HTTPError{module: Mint.HTTP2, reason: :closed}} ->
        state = put_in(state.requests, %{})

        state =
          case connect(state.server) do
            {:ok, conn} ->
              state = do_request(stream, mode, path, input_type, output_type, schema, from, state)
              put_in(state.conn, conn)

            {:error, reason} ->
              GenServer.reply(from, {:error, reason})
              put_in(state.conn, conn)
          end

        state

      {:error, conn, reason} ->
        state = put_in(state.conn, conn)
        GenServer.reply(from, {:error, reason})
        state
    end
  end

  @impl true
  def handle_cast(
        {:send, request_ref, :eof},
        state
      ) do
    case send_data(state, request_ref, :eof) do
      {:ok, state} ->
        state

      {:error, state, reason} ->
        reply_sender(state.requests[request_ref].from, {:error, reason})
        state
    end

    {:noreply, state}
  end

  @impl true
  def handle_cast(
        {:send, request_ref, data},
        %__MODULE__{requests: requests} = state
      ) do
    request = requests[request_ref]

    state =
      if not is_nil(request) do
        case Serde.encode(request.schema, request.input_type, data) do
          {:ok, data} ->
            case send_data(state, request_ref, to_data(data)) do
              {:ok, state} ->
                if request.mode == :unary do
                  send_data(state, request_ref, :eof) |> elem(1)
                else
                  state
                end

              {:error, state, reason} ->
                reply_sender(request.from, {:error, reason})
                state
            end

          error ->
            reply_sender(request.from, error)

            state
        end
      else
        state
      end

    {:noreply, state}
  end

  defp send_data(state, request_ref, data) do
    case Mint.HTTP2.stream_request_body(state.conn, request_ref, data) do
      {:ok, conn} ->
        {:ok, put_in(state.conn, conn)}

      {:error, conn, reason} ->
        {:error, put_in(state.conn, conn), reason}
    end
  end

  @impl true
  def handle_info(message, state) do
    case Mint.HTTP2.stream(state.conn, message) do
      :unknown ->
        _ = Logger.warn(fn -> "Received unknown message: " <> inspect(message) end)
        {:noreply, state}

      {:error, conn, reason, _} ->
        state = put_in(state.conn, conn)
        state = put_in(state.requests, %{})
        Logger.debug(Exception.message(reason))
        {:noreply, state}

      {:ok, conn, responses} ->
        state = put_in(state.conn, conn)
        state = Enum.reduce(responses, state, &process_response/2)
        {:noreply, state}
    end
  end

  defp process_response({:status, request_ref, status}, state) do
    if status != 200 do
      reply_sender(
        state.requests[request_ref].from,
        {:error,
         Error.new(
           UGRPC.Status.internal(),
           "status got is #{status} instead of 200"
         )}
      )
    end

    state
  end

  defp process_response({:headers, request_ref, headers}, state) do
    request = state.requests[request_ref]
    headers = Enum.into(headers, %{})

    case headers["grpc-status"] do
      status when is_binary(status) and status != "0" ->
        reply_sender(
          request.from,
          {:error,
           Error.new(
             String.to_integer(status),
             headers["grpc-message"]
           )}
        )

      _ ->
        nil
    end

    state
  end

  defp process_response({:data, request_ref, new_data}, state) do
    request = state.requests[request_ref]
    result = Serde.decode(request.schema, request.output_type, from_data(new_data))
    reply_sender(request.from,  result)
    state
  end

  defp process_response({:done, request_ref}, state) do
    {%{from: {from, _}}, state} = pop_in(state.requests[request_ref])
    reply_sender(from, :done)
    state
  end

  defp to_data(message) do
    {compressed, message} =
      if GzipCompressor.enabled?() do
        {1, GzipCompressor.compress(message)}
      else
        {0, message}
      end

    length = byte_size(message)
    <<compressed, length::size(4)-unit(8), message::binary>>
  end

  defp from_data(data) do
    <<compressed, _::bytes-size(4), data::binary>> = data

    if compressed == 1 && GzipCompressor.enabled?() do
      GzipCompressor.decompress(data)
    else
      data
    end
  end

  defp reply_sender({from, _}, response), do: reply_sender(from, response)
  defp reply_sender(from, response), do: send(from, {:stream, response})
end
