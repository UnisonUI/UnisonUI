defmodule TestGRPC.Endpoint do
  use GRPC.Endpoint

  intercept(GRPC.Logger.Server, level: :debug)
  run(TestGRPC.TestGrpcServer)
end
