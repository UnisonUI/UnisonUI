defmodule GitProvider.Git.SpecificationTest do
  use ExUnit.Case, async: true
  alias GitProvider.Git.{Configuration, Specification}

  describe "from_configuration/4" do
    test "from an openapi configuration" do
      specifications = [
        [name: "name", use_proxy: false, path: "file1"],
        [name: nil, use_proxy: nil, path: "file2"]
      ]

      config = %Configuration.OpenApi{use_proxy: true, specifications: specifications}
      result = Specification.from_configuration(config, "/", "service", "repo")

      assert result == [
               {:openapi, "/file1", [name: "name", use_proxy: false, path: "file1"]},
               {:openapi, "/file2", [name: "service", use_proxy: true, path: "file2"]}
             ]
    end

    test "from a grpc configuration" do
      files = [{"file1", [name: "name", servers: [:b]]}, {"file2", [name: nil, servers: []]}]

      config = %Configuration.Grpc{servers: [:a], files: files}
      result = Specification.from_configuration(config, "/", "service", "repo")

      assert result == [
               {:grpc, "/file1", [name: "name", servers: [:b]]},
               {:grpc, "/file2", [name: "service", servers: [:a]]}
             ]
    end

    test "from an unknown type" do
      result = Specification.from_configuration(nil, "/", "service", "repo")

      assert result == []
    end
  end
end
