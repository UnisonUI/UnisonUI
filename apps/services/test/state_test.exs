defmodule Services.StateTest do
  use ExUnit.Case, async: true
  alias Services.{Event, OpenApi, State}

  describe "reduce/2" do
    setup do
      service = %OpenApi{id: "test", name: "test", content: "test"}
      [service: service]
    end

    test "new service", context do
      state = State.new()
      event = %Event.Up{service: context.service}
      result = State.reduce(state, event)

      assert result == {%State{services: %{"test" => context.service}}, [event]}
    end

    test "name changed", context do
      state = %State{services: %{"test" => context.service}}
      new_service = %OpenApi{context.service | name: "test2"}
      event_up = %Event.Up{service: new_service}
      event_down = %Event.Down{id: context.service.id}
      result = State.reduce(state, %Event.Up{service: new_service})

      assert result ==
               {%State{services: %{"test" => new_service}}, [event_down, event_up]}
    end

    test "content changed", context do
      state = %State{services: %{"test" => context.service}}
      new_service = %OpenApi{context.service | content: "test2"}
      result = State.reduce(state, %Event.Up{service: new_service})

      assert result ==
               {%State{services: %{"test" => new_service}}, [%Event.Changed{id: "test"}]}
    end
  end
end
