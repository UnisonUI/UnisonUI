defmodule GitProvider.Git.EventsTest do
  use ExUnit.Case, async: true
  use ExUnitProperties

  import OK, only: [success: 1, failure: 1]

  alias Services.{OpenApi, Grpc, Metadata}
  alias Services.Event.{Down, Up}
  alias GitProvider.Git.{Events, Repository, Specifications}

  @repo %Repository{}

  describe "from_configuration/2" do
    test "the specifications are valid" do
      check all(types <- uniq_list_of(one_of([constant(:openapi), constant(:grpc)]), length: 2)) do
        specifications = %Specifications{
          specifications:
            Enum.into(types, %{}, fn type ->
              {to_string(type), {type, []}}
            end)
        }

        expected =
          Enum.map(types, fn
            :grpc ->
              %Events.Upsert.Grpc{
                path: "grpc",
                specs: [],
                repository: @repo
              }

            type ->
              %Events.Upsert.AsyncOpenApi{
                type: type,
                path: to_string(type),
                specs: [],
                repository: @repo
              }
          end)

        events = Events.from_specifications(specifications, @repo)

        assert Enum.all?(events, fn e -> Enum.find(expected, &(&1 == e)) != nil end)
      end
    end
  end

  describe "load_content/1" do
    test "openapi file exists" do
      event =
        Events.load_content(%Events.Upsert.AsyncOpenApi{
          type: :openapi,
          path: "test/git/specifications/openapi.yaml",
          specs: [],
          repository: @repo
        })

      assert event ==
               success(%Events.Upsert.AsyncOpenApi{
                 type: :openapi,
                 path: "test/git/specifications/openapi.yaml",
                 specs: [],
                 content: ~s/openapi: "3.1.0"\n/,
                 repository: @repo
               })
    end

    test "openapi file does not exist" do
      event =
        Events.load_content(%Events.Upsert.AsyncOpenApi{
          type: :openapi,
          path: "unknown",
          specs: [],
          repository: @repo
        })

      assert event == failure(:enoent)
    end

    test "delete event" do
      event = Events.load_content(%Events.Delete{path: "test", repository: @repo})

      assert event == {:ok, %GitProvider.Git.Events.Delete{path: "test", repository: @repo}}
    end
  end

  describe "from/1" do
    test "delete event" do
      assert Services.Event.from(%Events.Delete{
               path: "test",
               repository: %Repository{
                 name: "test",
                 directory: "/"
               }
             }) ==
               %Down{id: "test:test"}
    end

    test "upsert openapi" do
      assert Services.Event.from(%Events.Upsert.AsyncOpenApi{
               type: :openapi,
               path: "/openapi.yaml",
               specs: [name: "test", use_proxy: false],
               content: "test",
               repository: %Repository{
                 name: "test",
                 directory: "/",
                 uri: "file:///test"
               }
             }) ==
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
      assert Services.Event.from(%Events.Upsert.Grpc{
               path: "/helloworld.proto",
               specs: [name: "test", servers: []],
               schema: %{},
               repository: %Repository{
                 name: "test",
                 directory: "/",
                 uri: "file:///test"
               }
             }) ==
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
