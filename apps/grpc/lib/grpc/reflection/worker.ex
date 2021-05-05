defmodule GRPC.Reflection.Worker do
  use GenStateMachine, callback_mode: :state_functions

  @reflection_schema Protobuf.compile!("priv/reflection.proto")
  @service "grpc.reflection.v1alpha.ServerReflection"
  @method "ServerReflectionInfo"

  @list_services_request %{"message_request" => %{"type" => "list_services", "value" => "*"}}

  def start_link(_), do: GenStateMachine.start_link(__MODULE__, nil)

  @impl true
  def init(_), do: {:ok, :load_schema, nil}

  defp list_files_request(symbol),
    do: %{"message_request" => %{"type" => "file_containing_symbol", "value" => symbol}}

  @spec load_schema(pid :: pid(), server :: String.t()) ::
          {:ok, Protobuf.Structs.Schema.t()} | {:error, term()}
  def load_schema(pid, server), do: GenStateMachine.call(pid, server)

  def load_schema({:call, from}, server, _state) do
    with {:ok, client} <- GRPC.new_client(server),
         {:ok, request} <- GRPC.Client.request(client, @reflection_schema, @service, @method) do
      GRPC.Client.send_data(request, @list_services_request)
      {:next_state, :handle_services_listing, {from, request}}
    else
      error ->
        stop(from, error)
    end
  end

  def handle_services_listing(:info, {:stream, {:ok, response}}, {_from, request} = state) do
    response["message_response"]["value"]["service"]
    |> Stream.map(& &1["name"])
    |> Stream.reject(&(&1 == "grpc.reflection.v1alpha.ServerReflection"))
    |> Stream.map(&list_files_request/1)
    |> Enum.each(&GRPC.Client.send_data(request, &1))

    {:next_state, :handle_file_descriptors, state}
  end

  def handle_services_listing(:info, {:stream, error}, {from, _}), do: stop(from, error)

  def handle_file_descriptors(:info, {:stream, {:ok, response}}, {from, request}) do
    result =
      response["message_response"]["value"]["file_descriptor_proto"]
      |> Enum.map(&Base.decode64!/1)
      |> Protobuf.from_file_descriptors()

    GenStateMachine.reply({:reply, from, result})
    GRPC.Client.close(request)
    {:next_state, :close, request}
  end

  def handle_file_descriptors(:info, {:stream, error}, {from, _}), do: stop(from, error)

  def close(:info, _, _state), do: :stop

  defp stop(from, response), do: {:stop_and_reply, :normal, [{:reply, from, response}]}
end
