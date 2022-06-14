defmodule GRPCTest do
  alias GRPC.Protobuf
  use ExUnit.Case

  @schema Protobuf.compile!("test/protobuf/helloworld_stream.proto")

  defp free_port do
    {:ok, listen} = :gen_tcp.listen(0, [])
    {:ok, port} = :inet.port(listen)
    :gen_tcp.close(listen)
    port
  end

  setup_all do
    Application.put_env(:grpc, :start_server, true)
    port = free_port()
    start_supervised!({GRPC.Server.Supervisor, {TestGRPC.Endpoint, port}})
    {:ok, port: port}
  end

  describe "connect/1" do
    test "trying to connect to a non running server" do
      assert {:error, %Mint.TransportError{reason: :econnrefused}} ==
               GRPC.Client.new("http://localhost:#{free_port()}")
    end
  end

  describe "request/4" do
    test "an invalid method or service", context do
      {:ok, connection} = GRPC.Client.new("http://localhost:#{context[:port]}")

      assert {:error, :not_found} ==
               GRPC.Client.request(connection, @schema, "helloworld.Greeter", "SayByebye")
    end
  end

  describe "non streaming" do
    test "with non error", context do
      {:ok, connection} = GRPC.Client.new("http://localhost:#{context[:port]}")

      {:ok, connection} =
        GRPC.Client.request(connection, @schema, "helloworld.Greeter", "SayHello")

      GRPC.Client.send_data(connection, %{"name" => "world"})
      assert_receive {:stream, {:ok, %{"message" => "Hello world"}}}
      assert_receive {:stream, :done}
    end
  end

  describe "streaming" do
    test "with non error", context do
      {:ok, connection} = GRPC.Client.new("http://localhost:#{context[:port]}")

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
