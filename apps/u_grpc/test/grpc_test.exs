defmodule GRPCTest do
  alias UGRPC.Protobuf
  use ExUnit.Case

  @schema Protobuf.compile!("test/protobuf/helloworld_stream.proto")

  setup_all do
    Application.put_env(:grpc, :start_server, true)
    start_supervised!({GRPC.Server.Supervisor, {UGRPC.Endpoint, 50051}})
    :ok
  end

  describe "connect/1" do
    test "trying to connect to a non running server" do
      assert {:error, %Mint.TransportError{reason: :econnrefused}} ==
               UGRPC.new_client("http://localhost:4242")
    end
  end

  describe "request/4" do
    test "an invalid method or service" do
      {:ok, connection} = UGRPC.new_client("http://localhost:50051")

      assert {:error, :not_found} ==
               UGRPC.Client.request(connection, @schema, "helloworld.Greeter", "SayByebye")
    end
  end

  describe "non streaming" do
    test "with non error" do
      {:ok, connection} = UGRPC.new_client("http://localhost:50051")

      {:ok, connection} =
        UGRPC.Client.request(connection, @schema, "helloworld.Greeter", "SayHello")

      UGRPC.Client.send_data(connection, %{"name" => "world"})
      assert_receive {:stream, {:ok, %{"message" => "Hello world"}}}
      assert_receive {:stream, :done}
    end
  end

  describe "streaming" do
    test "with non error" do
      {:ok, connection} = UGRPC.new_client("http://localhost:50051")

      {:ok, connection} =
        UGRPC.Client.request(connection, @schema, "helloworld.Greeter", "SayHelloToAll")

      UGRPC.Client.send_data(connection, %{"name" => "world"})
      UGRPC.Client.send_data(connection, %{"name" => "UnisonUI"})
      UGRPC.Client.close(connection)

      assert_receive {:stream, {:ok, %{"message" => "Hello world"}}}
      assert_receive {:stream, {:ok, %{"message" => "Hello UnisonUI"}}}

      assert_receive {:stream, :done}
    end
  end
end
