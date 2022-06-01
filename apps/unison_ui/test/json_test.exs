defmodule UnisonUI.JsonTest do
  use ExUnit.Case, async: true
  alias Services.{Event, Service}

  describe "Jason.encode/2" do
    test "Up event with a grpc service" do
      service = %Service.Grpc{id: "test", name: "test", schema: "test"}

      assert Jason.encode(%Event.Up{service: service}) ==
               {:ok,
                "{\"event\":\"serviceUp\",\"id\":\"test\",\"metadata\":{\"file\":null,\"provider\":null},\"name\":\"test\",\"schema\":\"test\",\"servers\":[],\"type\":\"grpc\"}"}
    end

    test "Up event with an asyncapi service" do
      service = %Service.AsyncApi{id: "test", name: "test", content: "test"}

      assert Jason.encode(%Event.Up{service: service}) ==
               {:ok,
                "{\"content\":\"test\",\"event\":\"serviceUp\",\"id\":\"test\",\"metadata\":{\"file\":null,\"provider\":null},\"name\":\"test\",\"type\":\"asyncapi\",\"useProxy\":false}"}
    end

    test "Up event with an openapi service" do
      service = %Service.OpenApi{id: "test", name: "test", content: "test"}

      assert Jason.encode(%Event.Up{service: service}) ==
               {:ok,
                "{\"content\":\"test\",\"event\":\"serviceUp\",\"id\":\"test\",\"metadata\":{\"file\":null,\"provider\":null},\"name\":\"test\",\"type\":\"openapi\",\"useProxy\":false}"}
    end

    test "Down event" do
      assert Jason.encode(%Event.Down{id: "test"}) ==
               {:ok, "{\"event\":\"serviceDown\",\"id\":\"test\"}"}
    end

    test "Changed event" do
      assert Jason.encode(%Event.Changed{id: "test"}) ==
               {:ok, "{\"event\":\"serviceChanged\",\"id\":\"test\"}"}
    end
  end
end
