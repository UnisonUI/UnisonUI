defmodule GitProvider.Git.EventsTest do
  use ExUnit.Case, async: true
  use ExUnitProperties

  import OK, only: [success: 1, failure: 1]

  alias Common.Service.{OpenApi, Grpc, Metadata}
  alias Common.Events.{Down, Up}
  alias GitProvider.Git.{Event, Events, Repository, Specifications}

  describe "from_configuration/1" do
    test "the specifications are valid" do
      check all types <- uniq_list_of(one_of([constant(:openapi), constant(:grpc)]), length: 2) do
        specifications = %Specifications{
          specifications:
            Enum.into(types, %{}, fn type ->
              {to_string(type), {type, []}}
            end)
        }

        expected =
          Enum.map(types, fn
            :openapi ->
              %Events.Upsert.OpenApi{
                path: "openapi",
                specs: []
              }

            :grpc ->
              %Events.Upsert.Grpc{
                path: "grpc",
                specs: []
              }
          end)

        events = Events.from_specifications(specifications)

        assert Enum.all?(events, fn e -> Enum.find(expected, &(&1 == e)) != nil end)
      end
    end
  end

  describe "load_content/1" do
    test "openapi file exists" do
      event =
        Event.load_content(%Events.Upsert.OpenApi{
          path: "test/git/specifications/openapi.yaml",
          specs: []
        })

      assert event ==
               success(%Events.Upsert.OpenApi{
                 path: "test/git/specifications/openapi.yaml",
                 specs: [],
                 content: ~s/openapi: "3.1.0"\n/
               })
    end

    test "openapi file does not exist" do
      event =
        Event.load_content(%Events.Upsert.OpenApi{
          path: "unknown",
          specs: []
        })

      assert event == failure(:enoent)
    end

    test "delete event" do
      event = Event.load_content(%Events.Delete{path: "test"})

      assert event == {:ok, %GitProvider.Git.Events.Delete{path: "test"}}
    end
  end

  describe "to_event/2" do
    test "delete event" do
      assert Event.to_event(%Events.Delete{path: "test"}, %Repository{
               name: "test",
               directory: "/"
             }) ==
               %Down{id: "test:test"}
    end

    test "upsert openapi" do
      assert Event.to_event(
               %Events.Upsert.OpenApi{
                 path: "/openapi.yaml",
                 specs: [name: "test", use_proxy: false],
                 content: "test"
               },
               %Repository{
                 name: "test",
                 directory: "/",
                 uri: "file:///test"
               }
             ) ==
               %Up{
                 service: %OpenApi{
                   id: "test:openapi.yaml",
                   name: "test",
                   content: "test",
                   use_proxy: false,
                   metadata: %Metadata{provider: "local", file: "openapi.yaml"}
                 }
               }
    end

    test "upsert grpc" do
      assert Event.to_event(
               %Events.Upsert.Grpc{
                 path: "/helloworld.proto",
                 specs: [name: "test", servers: []],
                 schema: %{}
               },
               %Repository{
                 name: "test",
                 directory: "/",
                 uri: "file:///test"
               }
             ) ==
               %Up{
                 service: %Grpc{
                   id: "test:helloworld.proto",
                   name: "test",
                   schema: %{},
                   servers: [],
                   metadata: %Metadata{provider: "local", file: "helloworld.proto"}
                 }
               }
    end
  end
end
