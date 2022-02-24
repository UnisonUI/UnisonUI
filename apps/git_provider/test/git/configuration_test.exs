defmodule GitProvider.Git.ConfigurationTest do
  alias GitProvider.Git.Configuration
  alias GitProvider.Git.Configuration.{Grpc, AsyncOpenApi}
  alias Services.Service.Grpc.Server
  use ExUnit.Case

  test "parse valid v2" do
    {_, struct} = Configuration.from_file("test/git/configurations/valid_v2.yaml")

    assert(
      struct == %Configuration{
        version: "2",
        name: "test",
        openapi: %AsyncOpenApi{
          specifications: [
            %AsyncOpenApi.Specification{path: "file.yaml", name: nil, use_proxy: false},
            %AsyncOpenApi.Specification{
              path: "other.yaml",
              name: "another service",
              use_proxy: false
            }
          ],
          use_proxy: true
        },
        asyncapi: %AsyncOpenApi{
          specifications: [
            %AsyncOpenApi.Specification{path: "file.yaml", name: nil, use_proxy: false},
            %AsyncOpenApi.Specification{
              path: "other.yaml",
              name: "another service",
              use_proxy: false
            }
          ],
          use_proxy: true
        },
        grpc: %Grpc{
          files: %{
            "path/spec.proto" => %Grpc.Specification{
              name: nil,
              servers: %{
                "127.0.0.1:8080" => %Server{address: "127.0.0.1", port: 8080, use_tls: false}
              }
            },
            "path/spec2.proto" => %Grpc.Specification{
              name: "test",
              servers: %{
                "other server" => %Server{
                  address: "127.0.0.1",
                  port: 8080,
                  use_tls: true
                }
              }
            }
          }
        }
      }
    )
  end

  test "cannot parse v1 anymore" do
    result = Configuration.from_file("test/git/configurations/valid_v1.yaml")
    assert(match?({:error, %TypeCheck.TypeError{}}, result))
  end
end
