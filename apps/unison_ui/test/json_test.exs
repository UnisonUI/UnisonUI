defmodule UnisonUI.JsonTest do
  use ExUnit.Case, async: true
  alias Services.{Grpc, OpenApi}
  alias Services.Event.{Up, Down, Changed}

  describe "Jason.encode/2" do
    test "Up event with a grpc service" do
      service = %Grpc{id: "test", name: "test", schema: "test"}

      assert Jason.encode(%Up{service: service}) ==
               {:ok,
                "{\"event\":\"serviceUp\",\"id\":\"test\",\"metadata\":{\"file\":null,\"provider\":null},\"name\":\"test\",\"type\":\"grpc\"}"}
    end

    test "Up event with an openapi service" do
      service = %OpenApi{id: "test", name: "test", content: "test"}

      assert Jason.encode(%Up{service: service}) ==
               {:ok,
                "{\"event\":\"serviceUp\",\"id\":\"test\",\"metadata\":{\"file\":null,\"provider\":null},\"name\":\"test\",\"type\":\"openapi\",\"useProxy\":false}"}
    end

    test "Down event" do
      assert Jason.encode(%Down{id: "test"}) ==
               {:ok, "{\"event\":\"serviceDown\",\"id\":\"test\"}"}
    end

    test "Changed event" do
      assert Jason.encode(%Changed{id: "test"}) ==
               {:ok, "{\"event\":\"serviceChanged\",\"id\":\"test\"}"}
    end
  end
end
