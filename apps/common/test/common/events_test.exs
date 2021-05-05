defmodule Common.EventsTest do
  use ExUnit.Case
  alias Common.Service.Grpc
  alias Common.Events.{Up, Down, Changed}

  describe "Jason.encode/2" do
    test "Up event" do
      service = %Grpc{id: "test", name: "test", schema: "test"}

      assert Jason.encode(%Up{service: service}) ==
               {:ok,
                "{\"event\":\"serviceUp\",\"id\":\"test\",\"metadata\":{\"file\":null,\"provider\":null},\"name\":\"test\",\"type\":\"grpc\"}"}
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
