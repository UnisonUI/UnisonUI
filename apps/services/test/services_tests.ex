defmodule ServicesTest do
  use ExUnit.Case
  alias Services.{Grpc, Hash, Metadata, OpenApi}

  describe "Services.Hash.compute_hash/1" do
    test "GRPC service" do
      service = %Grpc{id: "test", name: "test", schema: "test"}
      assert Hash.compute_hash(service) == "a94a8fe5ccb19ba61c4c0873d391e987982fbbd3"
    end

    test "OpenApi service" do
      service = %OpenApi{id: "test", name: "test", content: "test"}
      assert Hash.compute_hash(service) == "a94a8fe5ccb19ba61c4c0873d391e987982fbbd3"
    end
  end

  describe "Services.to_event/1" do
    test "GRPC service" do
      service = %Grpc{id: "test", name: "test", schema: "test"}

      assert Service.to_event(service) == %{
               id: "test",
               name: "test",
               metadata: %Metadata{file: nil, provider: nil},
               type: :grpc
             }
    end

    test "OpenApi service" do
      service = %OpenApi{id: "test", name: "test", content: "test"}

      assert Service.to_event(service) == %{
               id: "test",
               name: "test",
               metadata: %Metadata{file: nil, provider: nil},
               useProxy: false,
               type: :openapi
             }
    end
  end
end
