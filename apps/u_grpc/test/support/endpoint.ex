defmodule UGRPC.Endpoint do
  use GRPC.Endpoint

  run(UGRPC.TestGrpcServer)
end
