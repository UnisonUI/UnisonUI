defmodule ContainerProvider.SpecificationTest do
  alias ContainerProvider.{HttpClient, Specifications}
  alias Services.{Grpc, OpenApi, Metadata}

  use ExUnit.Case, async: true
  import Mock

  describe "retrieve_specification/3 from OpenAPI" do
    test_with_mock "couldn't retrieve data", HttpClient, download_file: fn _endpoint -> nil end do
      assert Specifications.retrieve_specification("test", "service",
               endpoint: "http://localhost/openapi.yaml",
               use_proxy: false
             ) == nil
    end

    test_with_mock "retrieving data", HttpClient, download_file: fn _endpoint -> "data" end do
      result =
        Specifications.retrieve_specification("test", "service",
          endpoint: "http://localhost/openapi.yaml",
          use_proxy: false
        )

      assert result == %OpenApi{
               id: "test",
               name: "service",
               content: "data",
               use_proxy: false,
               metadata: %Metadata{provider: "container", file: "openapi.yaml"}
             }
    end
  end

  describe "retrieve_specification/3 from GRPC" do
    test_with_mock "couldn't retrieve data", GRPC.Reflection,
      load_schema: fn _endpoint -> {:error, GRPC.Error.new(2, "error")} end do
      assert Specifications.retrieve_specification("test", "service",
               address: "localhost",
               port: 80,
               use_tls: false
             ) == nil
    end

    test_with_mock "retrieving data", GRPC.Reflection,
      load_schema: fn _endpoint -> {:ok, "data"} end do
      result =
        Specifications.retrieve_specification("test", "service",
          address: "localhost",
          port: 80,
          use_tls: false
        )

      assert result == %Grpc{
               id: "test",
               name: "service",
               schema: "data",
               servers: %{"localhost:80" => [address: "localhost", port: 80, use_tls: false]},
               metadata: %Metadata{provider: "container", file: "localhost:80"}
             }
    end
  end
end
