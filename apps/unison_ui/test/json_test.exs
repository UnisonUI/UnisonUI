defmodule UnisonUI.JsonTest do
  use ExUnit.Case, async: true
  alias Services.{Event, Service}

  describe "Jason.encode/2" do
    test "Up event with a grpc service" do
      service = %Service.Grpc{id: "test", name: "test", schema: "test"}

      assert Jason.encode(%Event.Up{service: service}) ==
               {:ok,
                "{\"event\":\"serviceUp\",\"id\":\"test\",\"metadata\":{},\"name\":\"test\",\"schema\":\"test\",\"servers\":[],\"type\":\"grpc\"}"}
    end

    test "Up event with an asyncapi service" do
      service = %Service.AsyncApi{id: "test", name: "test", content: "test"}

      assert Jason.encode(%Event.Up{service: service}) ==
               {:ok,
                "{\"content\":\"test\",\"event\":\"serviceUp\",\"id\":\"test\",\"metadata\":{},\"name\":\"test\",\"type\":\"asyncapi\"}"}
    end

    test "Up event with an openapi service" do
      service = %Service.OpenApi{id: "test", name: "test", content: "test"}

      assert Jason.encode(%Event.Up{service: service}) ==
               {:ok,
                "{\"content\":\"test\",\"event\":\"serviceUp\",\"id\":\"test\",\"metadata\":{},\"name\":\"test\",\"type\":\"openapi\",\"use_proxy\":false}"}
    end

    test "Down event" do
      assert Jason.encode(%Event.Down{id: "test"}) ==
               {:ok, "{\"event\":\"serviceDown\",\"id\":\"test\"}"}
    end

    test "Changed event" do
      service = %Service.OpenApi{id: "test", name: "test", content: "test"}

      assert Jason.encode(%Event.Changed{service: service}) ==
               {:ok,
                "{\"content\":\"test\",\"event\":\"serviceChanged\",\"id\":\"test\",\"metadata\":{},\"name\":\"test\",\"type\":\"openapi\",\"use_proxy\":false}"}
    end
  end
end
