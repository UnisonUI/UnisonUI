defmodule GitProvider.Git.SpecificationTest do
  use ExUnit.Case, async: true
  use ExUnitProperties

  alias GitProvider.Git.{Configuration, Specifications}

  describe "from_configuration/4" do
    test "from an openapi configuration" do
      specifications = [
        [name: "name", use_proxy: false, path: "file1"],
        [name: nil, use_proxy: nil, path: "file2"]
      ]

      config = %Configuration.OpenApi{use_proxy: true, specifications: specifications}
      result = Specifications.from_configuration(config, "/", "service", "repo")

      assert result == %Specifications{
               specifications: %{
                 "/file1" =>
                   {:openapi,  [name: "name", use_proxy: false, path: "file1"]},
                 "/file2" =>
                   {:openapi,  [name: "service", use_proxy: true, path: "file2"]}
               }
             }
    end

    test "from a grpc configuration" do
      files = [{"file1", [name: "name", servers: [:b]]}, {"file2", [name: nil, servers: []]}]

      config = %Configuration.Grpc{servers: [:a], files: files}
      result = Specifications.from_configuration(config, "/", "service", "repo")

      assert result == %Specifications{
               specifications: %{
                 "/file1" => {:grpc,  [name: "name", servers: [:b]]},
                 "/file2" => {:grpc,  [name: "service", servers: [:a]]}
               }
             }
    end

    test "from an unknown type" do
      result = Specifications.from_configuration(nil, "/", "service", "repo")

      assert result == %Specifications{}
    end
  end

  describe "intersection/2" do
    property "difference between two specs" do
      check all path1 <- binary(),
              path2 <- binary(),
              path1 != path2 do
        specs_1 = %Specifications{specifications: %{path1 => {path1, nil, nil}}}

        specs_2 = %Specifications{
          specifications: %{path1 => {path1, nil, nil}, path2 => {path2, nil, nil}}
        }

        assert Specifications.intersection(specs_1, specs_2) == specs_1
      end
    end
  end
end
