defmodule GitProvider.Git.EventsTest do
  use ExUnit.Case, async: true

  import OK, only: [success: 1, failure: 1]

  alias Services.{Event, Service}
  alias GitProvider.Git.{Events, Repository, Specifications, Configuration}

  @repo %Repository{}

  describe "from_configuration/2" do
    test "the specifications are valid" do
      types = [:openapi, :grpc]

      specifications = %Specifications{
        specifications:
          Enum.into(types, %{}, fn type ->
            specs =
              case type do
                :grpc -> %Configuration.Grpc.Specification{}
                _ -> %Configuration.AsyncOpenApi.Specification{}
              end

            {to_string(type), {type, specs}}
          end)
      }

      expected =
        Enum.map(types, fn
          :grpc ->
            %Events.Upsert.Grpc{
              path: "grpc",
              specs: %Configuration.Grpc.Specification{},
              repository: @repo
            }

          type ->
            %Events.Upsert.AsyncOpenApi{
              type: type,
              path: to_string(type),
              specs: %Configuration.AsyncOpenApi.Specification{},
              repository: @repo
            }
        end)

      events = Events.from_specifications(specifications, @repo)

      assert Enum.all?(events, fn e -> Enum.find(expected, &(&1 == e)) != nil end)
    end
  end

  describe "load_content/1" do
    test "openapi file exists" do
      event =
        Events.load_content(%Events.Upsert.AsyncOpenApi{
          type: :openapi,
          path: "test/git_provider/git/specifications/openapi.yaml",
          specs: %Configuration.AsyncOpenApi.Specification{},
          repository: @repo
        })

      assert event ==
               success(%Events.Upsert.AsyncOpenApi{
                 type: :openapi,
                 path: "test/git_provider/git/specifications/openapi.yaml",
                 specs: %Configuration.AsyncOpenApi.Specification{},
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
      assert Event.from(%Events.Delete{
               path: "test",
               repository: %Repository{
                 name: "test",
                 directory: "/"
               }
             }) ==
               %Event.Down{id: "test:test"}
    end

    test "upsert openapi" do
      assert Event.from(%Events.Upsert.AsyncOpenApi{
               type: :openapi,
               path: "/openapi.yaml",
               specs: %Configuration.AsyncOpenApi.Specification{name: "test", use_proxy: false},
               content: "test",
               repository: %Repository{
                 name: "test",
                 directory: "/",
                 uri: "file:///test"
               }
             }) ==
               %Event.Up{
                 service: %Service.OpenApi{
                   id: "test:openapi.yaml",
                   name: "test",
                   content: "test",
                   use_proxy: false,
                   metadata: %Service.Metadata{provider: "git", file: "openapi.yaml"}
                 }
               }
    end

    test "upsert grpc" do
      assert Event.from(%Events.Upsert.Grpc{
               path: "/helloworld.proto",
               specs: %Configuration.Grpc.Specification{name: "test", servers: []},
               schema: %{},
               repository: %Repository{
                 name: "test",
                 directory: "/",
                 uri: "file:///test"
               }
             }) ==
               %Event.Up{
                 service: %Service.Grpc{
                   id: "test:helloworld.proto",
                   name: "test",
                   schema: %{},
                   servers: [],
                   metadata: %Service.Metadata{provider: "git", file: "helloworld.proto"}
                 }
               }
    end
  end
end
