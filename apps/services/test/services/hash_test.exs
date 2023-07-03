defmodule Services.ServiceTest do
  use ExUnit.Case
  alias Services.Service.{AsyncApi, Grpc, Hash, OpenApi}

  describe "Services.Hash.compute_hash/1" do
    test "GRPC service" do
      service = %Grpc{id: "test", name: "test", schema: "test"}
      assert Hash.compute_hash(service) == "a94a8fe5ccb19ba61c4c0873d391e987982fbbd3"
    end

    test "AsyncApi service" do
      service = %AsyncApi{id: "test", name: "test", content: "test"}
      assert Hash.compute_hash(service) == "a94a8fe5ccb19ba61c4c0873d391e987982fbbd3"
    end

    test "OpenApi service" do
      service = %OpenApi{id: "test", name: "test", content: "test"}
      assert Hash.compute_hash(service) == "a94a8fe5ccb19ba61c4c0873d391e987982fbbd3"
    end
  end
end
