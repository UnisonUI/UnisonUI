defmodule GitProvider.Git.ConfigurationTest do
  alias GitProvider.Git.Configuration
  alias GitProvider.Git.Configuration.{Grpc, AsyncOpenApi}
  use ExUnit.Case

  test "parse valid v1" do
    {_, struct} = Configuration.from_file("test/git/configurations/valid_v1.yaml")

    assert(
      struct == %Configuration{
        version: "1",
        name: "test",
        openapi: %AsyncOpenApi{
          specifications: [
            [path: "file.yaml", name: nil, use_proxy: false],
            [path: "other.yaml", name: "another service", use_proxy: false]
          ],
          use_proxy: true
        }
      }
    )
  end

  test "parse valid v2" do
    {_, struct} = Configuration.from_file("test/git/configurations/valid_v2.yaml")

    assert(
      struct == %Configuration{
        version: "2",
        name: "test",
        openapi: %AsyncOpenApi{
          specifications: [
            [path: "file.yaml", name: nil, use_proxy: false],
            [path: "other.yaml", name: "another service", use_proxy: false]
          ],
          use_proxy: true
        },
        asyncapi: %AsyncOpenApi{
          specifications: [
            [path: "file.yaml", name: nil, use_proxy: false],
            [path: "other.yaml", name: "another service", use_proxy: false]
          ],
          use_proxy: true
        },
        grpc: %Grpc{
          files: %{
            "path/spec.proto" => [
              name: nil,
              servers: %{
                "127.0.0.1:8080" => [address: "127.0.0.1", port: 8080, use_tls: false]
              }
            ],
            "path/spec2.proto" => [
              name: "test",
              servers: %{
                "other server" => [
                  address: "127.0.0.1",
                  port: 8080,
                  use_tls: true
                ]
              }
            ]
          }
        }
      }
    )
  end

  test "parse invalid v1" do
    {:error, struct} = Configuration.from_file("test/git/configurations/invalid_v1.yaml")

    assert(
      struct == [
        %{
          input: %Grpc{
            files: %{
              "path/spec.proto" => [
                name: nil,
                servers: %{"127.0.0.1:8080" => [address: "127.0.0.1", port: 8080, use_tls: false]}
              ],
              "path/spec2.proto" => [
                name: "test",
                servers: %{"other server" => [address: "127.0.0.1", port: 8080, use_tls: true]}
              ]
            },
            servers: nil
          },
          path: [:grpc],
          spec: "is_nil()"
        },
        %{input: "1", path: [:version], spec: "&(&1 != \"1\")"}
      ]
    )
  end
end
