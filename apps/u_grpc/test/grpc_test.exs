defmodule GRPCTest do
  alias GRPC.Protobuf
  use ExUnit.Case

  @schema Protobuf.compile!("test/protobuf/helloworld_stream.proto")

  setup_all do
    Application.put_env(:grpc, :start_server, true)
    start_supervised!({GRPC.Server.Supervisor, {GRPC.Endpoint, 50051}})
    :ok
  end

  describe "connect/1" do
    test "trying to connect to a non running server" do
      assert {:error, %Mint.TransportError{reason: :econnrefused}} ==
               GRPC.Client.new("http://localhost:4242")
    end
  end

  describe "request/4" do
    test "an invalid method or service" do
      {:ok, connection} = GRPC.Client.new("http://localhost:50051")

      assert {:error, :not_found} ==
               GRPC.Client.request(connection, @schema, "helloworld.Greeter", "SayByebye")
    end
  end

  describe "non streaming" do
    test "with non error" do
      {:ok, connection} = GRPC.Client.new("http://localhost:50051")

      {:ok, connection} =
        GRPC.Client.request(connection, @schema, "helloworld.Greeter", "SayHello")

      GRPC.Client.send_data(connection, %{"name" => "world"})
      assert_receive {:stream, {:ok, %{"message" => "Hello world"}}}
      assert_receive {:stream, :done}
    end
  end

  describe "streaming" do
    test "with non error" do
      {:ok, connection} = GRPC.Client.new("http://localhost:50051")

      {:ok, connection} =
        GRPC.Client.request(connection, @schema, "helloworld.Greeter", "SayHelloToAll")

      GRPC.Client.send_data(connection, %{"name" => "world"})
      GRPC.Client.send_data(connection, %{"name" => "UnisonUI"})
      GRPC.Client.close(connection)

      assert_receive {:stream, {:ok, %{"message" => "Hello world"}}}
      assert_receive {:stream, {:ok, %{"message" => "Hello UnisonUI"}}}

      assert_receive {:stream, :done}
    end
  end
end
