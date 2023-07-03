defmodule UnisonUI.JsonTest do
  use ExUnit.Case, async: true
  alias Services.{Event, Service}

  describe "Jason.encode/2" do
    test "Up event with a grpc service" do
      service = %Service.Grpc{id: "test", name: "test", schema: "test"}

      assert Jason.encode(%Event.Up{service: service}) ==
               {:ok,
                "{\"id\":\"test\",\"name\":\"test\",\"type\":\"grpc\",\"metadata\":{},\"servers\":[],\"event\":\"serviceUp\",\"schema\":\"test\"}"}
    end

    test "Up event with an asyncapi service" do
      service = %Service.AsyncApi{id: "test", name: "test", content: "test"}

      assert Jason.encode(%Event.Up{service: service}) ==
               {:ok,
                "{\"id\":\"test\",\"name\":\"test\",\"type\":\"asyncapi\",\"metadata\":{},\"content\":\"test\",\"event\":\"serviceUp\"}"}
    end

    test "Up event with an openapi service" do
      service = %Service.OpenApi{id: "test", name: "test", content: "test"}

      assert Jason.encode(%Event.Up{service: service}) ==
               {:ok,
                "{\"id\":\"test\",\"name\":\"test\",\"type\":\"openapi\",\"metadata\":{},\"content\":\"test\",\"use_proxy\":false,\"event\":\"serviceUp\"}"}
    end

    test "Down event" do
      assert Jason.encode(%Event.Down{id: "test"}) ==
               {:ok, "{\"id\":\"test\",\"event\":\"serviceDown\"}"}
    end

    test "Changed event" do
      service = %Service.OpenApi{id: "test", name: "test", content: "test"}

      assert Jason.encode(%Event.Changed{service: service}) ==
               {:ok,
                "{\"id\":\"test\",\"name\":\"test\",\"type\":\"openapi\",\"metadata\":{},\"content\":\"test\",\"use_proxy\":false,\"event\":\"serviceChanged\"}"}
    end
  end
end
