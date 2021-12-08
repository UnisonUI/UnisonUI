defmodule GRPC.TestGrpcServer do
  use GRPC.Server, service: Helloworld.Greeter.Service

  def say_hello(request, _stream), do: Helloworld.HelloReply.new(message: "Hello #{request.name}")

  def say_hello_to_all(requests, stream),
    do:
      Enum.reduce(requests, stream, fn %{name: name}, stream ->
        GRPC.Server.send_reply(stream, Helloworld.HelloReply.new(message: "Hello #{name}"))
      end)
end
