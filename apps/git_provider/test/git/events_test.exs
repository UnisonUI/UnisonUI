defmodule GitProvider.Git.EventsTest do
  use ExUnit.Case, async: true
  import OK, only: [success: 1, failure: 1]
  alias Common.Service.{OpenApi, Grpc, Metadata}
  alias Common.Events.{Down, Up}
  alias GitProvider.Git.{Event, Events, Repository, Specification}

  describe "from_configuration/1" do
    test "the specifications is an openapi one" do
      event =
        Events.from_specification(%Specification{
          type: :openapi,
          path: "test/git/specifications/openapi.yaml",
          specs: []
        })

      assert event == %Events.Upsert.Openapi{
               path: "test/git/specifications/openapi.yaml",
               specs: []
             }
    end

    test "the specifications is a grpc one" do
      event =
        Events.from_specification(%Specification{
          type: :grpc,
          path: "test/git/specifications/helloworld.proto",
          specs: []
        })

      assert event == %Events.Upsert.Grpc{
               path: "test/git/specifications/helloworld.proto",
               specs: []
             }
    end
  end

  describe "load_content/1" do
    test "openapi file exists" do
      event =
        Event.load_content(%Events.Upsert.Openapi{
          path: "test/git/specifications/openapi.yaml",
          specs: []
        })

      assert event ==
               success(%Events.Upsert.Openapi{
                 path: "test/git/specifications/openapi.yaml",
                 specs: [],
                 content: ~s/openapi: "3.1.0"\n/
               })
    end

    test "openapi file does not exist" do
      event =
        Event.load_content(%Events.Upsert.Openapi{
          path: "unknown",
          specs: []
        })

      assert event == failure(:enoent)
    end

    test "ignore" do
      event = Event.load_content(%Events.Delete{path: "test"})

      assert event == :ignore
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
               %Events.Upsert.Openapi{
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
