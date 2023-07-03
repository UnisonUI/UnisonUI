defmodule TestGRPC.TestGrpcServer do
  use GRPC.Server, service: Helloworld.Greeter.Service, compressors: [GRPC.Compressor.Gzip]

  def say_hello(%{name: name}, stream) do
    GRPC.Server.set_compressor(stream, GRPC.Compressor.Gzip)
    Helloworld.HelloReply.new(message: "Hello #{name}")
  end

  def say_hello_to_all(requests, stream) do
    GRPC.Server.set_compressor(stream, GRPC.Compressor.Gzip)

    Enum.reduce(requests, stream, fn %{name: name}, stream ->
      GRPC.Server.send_reply(stream, Helloworld.HelloReply.new(message: "Hello #{name}"))
    end)
  end
end
