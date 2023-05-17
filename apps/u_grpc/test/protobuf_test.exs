defmodule ProtobufTest do
  alias GRPC.Protobuf
  use ExUnit.Case

  describe "compile/1" do
    test "with a valid path" do
      result = Protobuf.compile("test/protobuf/helloworld.proto")
      assert match?({:ok, %Protobuf.Structs.Schema{}}, result)
    end

    test "with an invalid protobuf file" do
      assert Protobuf.compile("test/protobuf/invalid.proto") ==
               {:error,
                %Protobuf.ProtocError{
                  message:
                    "invalid.proto:1:10: Unrecognized syntax identifier \"proto4\".  This parser only recognizes \"proto2\" and \"proto3\"."
                }}
    end

    test "with an non-existing file" do
      assert Protobuf.compile("non-existing.proto") == {:error, :enoent}
    end
  end

  describe "encode/3" do
    test "with a valid type" do
      {:ok, schema} = Protobuf.compile("test/protobuf/helloworld.proto")

      result = Protobuf.encode(schema, "helloworld.HelloReply", %{"message" => "test"})

      assert result == {:ok, <<10, 4, 116, 101, 115, 116>>}
    end

    test "with an oneof field" do
      {:ok, schema} = Protobuf.compile("test/protobuf/helloworld.proto")

      result =
        Protobuf.encode(schema, "helloworld.HelloRequest", %{
          "name" => "test",
          "switch" => %{"type" => "myInt", "value" => 2}
        })

      assert result == {:ok, <<10, 4, 116, 101, 115, 116, 16, 2>>}
    end

    test "with a complex type using proto2" do
      {:ok, schema} = Protobuf.compile("test/protobuf/complex_proto2.proto")

      result =
        Protobuf.encode(schema, "helloworld.Complex", %{
          "myEnum" => ["VALUE1", "VALUE2"],
          "myMap" => %{"k" => "val", "o" => "a"},
          "myBytes" => "dGVzdAo=",
          "name" => "test",
          "tree" => %{
            "root" => true,
            "children" => [%{"value" => 1, "children" => []}, %{"value" => 2, "children" => []}]
          }
        })

      assert result ==
               {:ok,
                <<50, 5, 116, 101, 115, 116, 10, 18, 2, 0, 1, 26, 8, 10, 1, 107, 18, 3, 118, 97,
                  108, 26, 6, 10, 1, 111, 18, 1, 97, 10, 4, 116, 101, 115, 116, 34, 10, 18, 2, 8,
                  1, 18, 2, 8, 2, 24, 1>>}
    end

    test "with a complex type using proto3" do
      {:ok, schema} = Protobuf.compile("test/protobuf/complex_proto3.proto")

      result =
        Protobuf.encode(schema, "helloworld.Complex", %{
          "myEnum" => ["VALUE1", "VALUE2"],
          "myMap" => %{"k" => "val", "o" => "a"},
          "myBytes" => "dGVzdAo=",
          "name" => "test",
          "tree" => %{
            "root" => true,
            "children" => [%{"value" => 1, "children" => []}, %{"value" => 2, "children" => []}]
          },
          "myIntArray" => [1, 2, 3]
        })

      assert result ==
               {:ok,
                <<50, 5, 116, 101, 115, 116, 10, 18, 0, 18, 1, 58, 3, 1, 2, 3, 26, 8, 10, 1, 107,
                  18, 3, 118, 97, 108, 26, 6, 10, 1, 111, 18, 1, 97, 10, 4, 116, 101, 115, 116,
                  34, 10, 18, 2, 8, 1, 18, 2, 8, 2, 24, 1>>}
    end

    test "missing required field" do
      {:ok, schema} = Protobuf.compile("test/protobuf/complex_proto2.proto")

      result = Protobuf.encode(schema, "helloworld.Complex", %{"myInt" => 1})
      assert result == {:error, Protobuf.RequiredFieldError.exception("name")}
    end

    test "with an invalid type" do
      {:ok, schema} = Protobuf.compile("test/protobuf/helloworld.proto")

      result = Protobuf.encode(schema, "helloworld.HelloReply2", %{"message" => "test"})

      assert result == {:error, Protobuf.UnknownMessageError.exception("helloworld.HelloReply2")}
    end
  end

  describe "decode/3" do
    test "with a valid type" do
      {:ok, schema} = Protobuf.compile("test/protobuf/helloworld.proto")

      result = Protobuf.decode(schema, "helloworld.HelloReply", <<10, 4, 116, 101, 115, 116>>)

      assert result == {:ok, %{"message" => "test"}}
    end

    test "with an oneof field" do
      {:ok, schema} = Protobuf.compile("test/protobuf/helloworld.proto")

      result =
        Protobuf.decode(schema, "helloworld.HelloRequest", <<10, 4, 116, 101, 115, 116, 16, 2>>)

      assert result ==
               {:ok,
                %{
                  "name" => "test",
                  "switch" => %{"type" => "myInt", "value" => 2}
                }}
    end

    test "with a complex type with proto2" do
      {:ok, schema} = Protobuf.compile("test/protobuf/complex_proto2.proto")

      result =
        Protobuf.decode(
          schema,
          "helloworld.Complex",
          <<50, 5, 116, 101, 115, 116, 10, 18, 2, 0, 1, 26, 8, 10, 1, 107, 18, 3, 118, 97, 108,
            26, 6, 10, 1, 111, 18, 1, 97, 10, 4, 116, 101, 115, 116, 34, 10, 18, 2, 8, 1, 18, 2,
            8, 2, 24, 1>>
        )

      assert result ==
               {:ok,
                %{
                  "myEnum" => ["VALUE1", "VALUE2"],
                  "myMap" => %{"k" => "val", "o" => "a"},
                  "myBytes" => "dGVzdAo=",
                  "myInt" => 0,
                  "name" => "test",
                  "tree" => %{
                    "root" => true,
                    "value" => 0,
                    "children" => [
                      %{"value" => 1, "children" => [], "root" => false},
                      %{"value" => 2, "children" => [], "root" => false}
                    ]
                  }
                }}
    end

    test "with a complex type with proto3" do
      {:ok, schema} = Protobuf.compile("test/protobuf/complex_proto3.proto")

      result =
        Protobuf.decode(
          schema,
          "helloworld.Complex",
          <<50, 5, 116, 101, 115, 116, 10, 18, 0, 18, 1, 58, 3, 1, 2, 3, 26, 8, 10, 1, 107, 18, 3,
            118, 97, 108, 26, 6, 10, 1, 111, 18, 1, 97, 10, 4, 116, 101, 115, 116, 34, 10, 18, 2,
            8, 1, 18, 2, 8, 2, 24, 1>>
        )

      assert result ==
               {:ok,
                %{
                  "myEnum" => ["VALUE1", "VALUE2"],
                  "myMap" => %{"k" => "val", "o" => "a"},
                  "myBytes" => "dGVzdAo=",
                  "myInt" => 0,
                  "name" => "test",
                  "tree" => %{
                    "root" => true,
                    "value" => 0,
                    "children" => [
                      %{"value" => 1, "children" => [], "root" => false},
                      %{"value" => 2, "children" => [], "root" => false}
                    ]
                  },
                  "myIntArray" => [1, 2, 3]
                }}
    end

    test "with an invalid data" do
      {:ok, schema} = Protobuf.compile("test/protobuf/helloworld.proto")

      result = Protobuf.decode(schema, "helloworld.HelloReply", <<1>>)

      assert result ==
               {:error, Protobuf.UnknownFieldError.exception(0, "helloworld.HelloReply")}
    end

    test "with an invalid type" do
      {:ok, schema} = Protobuf.compile("test/protobuf/helloworld.proto")

      result = Protobuf.decode(schema, "helloworld.HelloReply2", <<10, 4, 116, 101, 115, 116>>)

      assert result == {:error, Protobuf.UnknownMessageError.exception("helloworld.HelloReply2")}
    end
  end
end
