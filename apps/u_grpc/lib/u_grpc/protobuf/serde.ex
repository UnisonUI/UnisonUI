defmodule UGRPC.Protobuf.Serde do
  alias UGRPC.Protobuf.Structs.{Schema, Field}

  @spec encode(schema :: UGRPC.Protobuf.Structs.Schema.t(), type :: String.t(), data :: map()) ::
          {:ok, binary()} | {:error, term()}
  def encode(%Schema{} = schema, type, data) do
    case schema.messages[type] do
      nil ->
        {:error, UGRPC.Protobuf.UnknownMessageError.exception(type)}

      message_schema ->
        case has_all_required_field(schema, type, data) do
          {:error, _} = error ->
            error

          _ ->
            fields = Enum.into(message_schema.fields, %{}, &map_field_by_name/1)

            result =
              data
              |> Stream.flat_map(fn kv -> get_field(kv, fields, message_schema) end)
              |> Enum.reduce(<<>>, fn {value, field}, protobuf_binary ->
                encode_field(schema, value, field, protobuf_binary)
              end)

            {:ok, result}
        end
    end
  end

  defp has_all_required_field(schema, type_name, data) do
    schema.messages[type_name].fields
    |> Enum.reduce_while(:ok, fn
      {_, %Field{label: :required, name: name, type: type, schema: type_name}}, _ ->
        unless Map.has_key?(data, name) do
          {:halt, {:error, UGRPC.Protobuf.RequiredFieldError.exception(name)}}
        else
          case type do
            :message ->
              result = has_all_required_field(schema, type_name, data[name])
              if result == :ok, do: {:cont, :ok}, else: {:halt, result}

            _ ->
              {:cont, :ok}
          end
        end

      _, _ ->
        {:cont, :ok}
    end)
  end

  defp map_field_by_name({_, %Field{name: name} = field}), do: {name, field}

  defp get_field({key, value}, fields, message) do
    case Map.get(fields, key) do
      nil ->
        case {is_map(value), Map.get(message.one_ofs, key)} do
          {true, one_ofs} when is_map(one_ofs) ->
            one_ofs
            |> Enum.flat_map(fn {_, %Field{name: name} = field} ->
              if name == value["type"], do: [[{value["value"], field}]], else: []
            end)
            |> Enum.at(0, [])

          _ ->
            []
        end

      field ->
        [{value, field}]
    end
  end

  defp encode_field(
         schema,
         value,
         %Field{
           id: number,
           type: type,
           schema: type_name,
           packed: packed,
           label: label
         },
         protobuf_binary
       ) do
    encoded =
      case {label, packed} do
        {:repeated, true} ->
          [encode_packed_list(schema, type_name, number, type, value)]

        {:repeated, false} ->
          [encode_list(schema, type_name, number, type, value)]

        _ ->
          wire_type = find_wire_type(type)

          case encode(type, value, schema, type_name) do
            {:ok, data} ->
              [Protox.Encode.make_key(number, wire_type), data]

            _ ->
              []
          end
      end

    [protobuf_binary, encoded] |> :binary.list_to_bin()
  end

  defp find_wire_type(:message), do: :packed
  defp find_wire_type(:enum), do: :packed
  defp find_wire_type(type), do: type

  defp encode_packed_list(schema, type_name, number, type, value) do
    {value, len} =
      Enum.reduce(value, {<<>>, 0}, fn value, {protobuf_binary, len} ->
        case encode(type, value, schema, type_name) do
          {:ok, data} -> {[protobuf_binary, data] |> :binary.list_to_bin(), len + 1}
          _ -> {protobuf_binary, len}
        end
      end)

    [
      Protox.Encode.make_key(number, :packed),
      Protox.Encode.encode_uint32(len),
      value
    ]
    |> :binary.list_to_bin()
  end

  defp encode_list(schema, type_name, number, type, value) do
    wire_type = find_wire_type(type)

    Enum.reduce(value, <<>>, fn value, protobuf_binary ->
      case encode(type, value, schema, type_name) do
        {:ok, data} ->
          [
            protobuf_binary,
            Protox.Encode.make_key(number, wire_type),
            data
          ]
          |> :binary.list_to_bin()

        _ ->
          protobuf_binary
      end
    end)
  end

  defp encode(type, value, schema, type_name) do
    try do
      data =
        case type do
          :fixed32 ->
            Protox.Encode.encode_fixed32(value)

          :sfixed32 ->
            Protox.Encode.encode_sfixed32(value)

          :float ->
            Protox.Encode.encode_float(value)

          :fixed64 ->
            Protox.Encode.encode_fixed64(value)

          :sfixed64 ->
            Protox.Encode.encode_sfixed64(value)

          :double ->
            Protox.Encode.encode_double(value)

          :int32 ->
            Protox.Encode.encode_int32(value)

          :uint32 ->
            Protox.Encode.encode_uint32(value)

          :sint32 ->
            Protox.Encode.encode_sint32(value)

          :int64 ->
            Protox.Encode.encode_sint64(value)

          :uint64 ->
            Protox.Encode.encode_uint64(value)

          :sint64 ->
            Protox.Encode.encode_sint64(value)

          :bool ->
            Protox.Encode.encode_bool(value)

          :string ->
            Protox.Encode.encode_string(value) |> :binary.list_to_bin()

          :bytes ->
            value |> Base.decode64!() |> Protox.Encode.encode_bytes() |> :binary.list_to_bin()

          :enum ->
            value =
              schema.enums[type_name].values
              |> Enum.find(&(elem(&1, 1) == value))
              |> elem(0)

            Protox.Encode.encode_enum(value)

          :message ->
            value =
              if is_tuple(value) do
                {k, v} = value
                %{"key" => k, "value" => v}
              else
                value
              end

            {:ok, value} = encode(schema, type_name, value)
            [Protox.Varint.encode(byte_size(value)), value] |> :binary.list_to_bin()
        end

      {:ok, data}
    rescue
      e -> {:error, e}
    end
  end

  @spec decode(
          schema :: Protobuf.Structs.Schema.t(),
          type_name :: String.t(),
          data :: binary(),
          result :: map()
        ) :: {:ok, map()} | {:error, term()}
  def decode(schema, type, data, result \\ %{})

  def decode(schema, type_name, data, result) when result == %{} do
    case schema.messages[type_name] do
      nil ->
        {:error, UGRPC.Protobuf.UnknownMessageError.exception(type_name)}

      message_schema ->
        result = initialise_result_with_default(message_schema)
        decode(schema, type_name, data, result)
    end
  end

  def decode(_schema, _type, <<>>, result), do: {:ok, result}

  def decode(%Schema{} = schema, type_name, data, result) do
    message_schema = schema.messages[type_name]
    {number, _type, rest} = Protox.Decode.parse_key(data)

    case Map.get(message_schema.fields, number) do
      nil ->
        {:error, UGRPC.Protobuf.UnknownFieldError.exception(number, type_name)}

      %Field{type: type, name: name, packed: packed, label: label, schema: schema_name} ->
        decode_result =
          case {label, packed} do
            {:repeated, false} ->
              decode_list(number, type, schema, schema_name, data, [])

            {:repeated, true} ->
              {len, rest} = Protox.Decode.parse_uint32(rest)

              result =
                Enum.reduce_while(1..len, {[], rest}, fn _, {list, rest} ->
                  case _decode(type, schema, schema_name, rest) do
                    {:ok, {value, rest}} -> {:cont, {[value | list], rest}}
                    error -> {:halt, error}
                  end
                end)

              case result do
                {:error, _} = error ->
                  error

                {list, rest} ->
                  {:ok, {Enum.reverse(list), rest}}
              end

            _ ->
              _decode(type, schema, schema_name, rest)
          end

        case decode_result do
          {:ok, {value, rest}} ->
            result =
              case is_one_of(message_schema, number) do
                nil -> Map.put(result, name, value)
                one_of -> Map.put(result, one_of, %{"type" => name, "value" => value})
              end

            decode(schema, type_name, rest, result)

          error ->
            error
        end
    end
  end

  defp is_one_of(message_schema, number) do
    message_schema.one_ofs
    |> Enum.flat_map(fn {one_of, types} ->
      case Map.get(types, number) do
        nil -> []
        _ -> [one_of]
      end
    end)
    |> Enum.at(0)
  end

  defp initialise_result_with_default(message_schema) do
    Enum.reduce(message_schema.fields, %{}, fn {number,
                                                %Field{
                                                  name: name,
                                                  type: type,
                                                  default: default,
                                                  label: label
                                                }},
                                               result ->
      case is_one_of(message_schema, number) do
        nil ->
          value =
            if label == :repeated do
              []
            else
              compute_default_value(type, default)
            end

          Map.put(result, name, value)

        one_of ->
          Map.put(result, one_of, nil)
      end
    end)
  end

  defp compute_default_value(:fixed32, nil), do: 0
  defp compute_default_value(:sfixed32, nil), do: 0
  defp compute_default_value(:fixed64, nil), do: 0
  defp compute_default_value(:sfixed64, nil), do: 0
  defp compute_default_value(:int32, nil), do: 0
  defp compute_default_value(:sint32, nil), do: 0
  defp compute_default_value(:uint32, nil), do: 0
  defp compute_default_value(:int64, nil), do: 0
  defp compute_default_value(:sint64, nil), do: 0
  defp compute_default_value(:float, nil), do: 0.0
  defp compute_default_value(:double, nil), do: 0.0
  defp compute_default_value(:bool, nil), do: false
  defp compute_default_value(:string, nil), do: ""
  defp compute_default_value(:bytes, nil), do: ""
  defp compute_default_value(_, nil), do: nil
  defp compute_default_value(_, default), do: default

  defp decode_list(_number, _type, schema, type_name, <<>>, result),
    do: {:ok, {result |> Enum.reverse() |> list_to_map(schema, type_name), <<>>}}

  defp decode_list(number, type, schema, type_name, data, result) do
    {tag, _type, rest} = Protox.Decode.parse_key(data)

    if tag != number do
      {:ok, {result |> Enum.reverse() |> list_to_map(schema, type_name), data}}
    else
      case _decode(type, schema, type_name, rest) do
        {:ok, {value, rest}} ->
          decode_list(number, type, schema, type_name, rest, [value | result])

        {:error, _} ->
          {result, rest}
      end
    end
  end

  defp list_to_map(value, _schema, nil), do: value
  defp list_to_map(value, schema, type_name) do
    map? = Map.get(schema.messages[type_name].options || %{}, "map_entry")

    if map? do
      Enum.into(value, %{}, fn %{"key" => key, "value" => value} -> {key, value} end)
    else
      value
    end
  end

  defp _decode(type, schema, type_name, data) do
    try do
      result =
        case type do
          :fixed32 ->
            Protox.Decode.parse_fixed32(data)

          :sfixed32 ->
            Protox.Decode.parse_sfixed32(data)

          :float ->
            Protox.Decode.parse_float(data)

          :fixed64 ->
            Protox.Decode.parse_fixed64(data)

          :sfixed64 ->
            Protox.Decode.parse_sfixed64(data)

          :double ->
            Protox.Decode.parse_double(data)

          :int32 ->
            Protox.Decode.parse_int32(data)

          :uint32 ->
            Protox.Decode.parse_uint32(data)

          :sint32 ->
            Protox.Decode.parse_sint32(data)

          :int64 ->
            Protox.Decode.parse_sint64(data)

          :uint64 ->
            Protox.Decode.parse_uint64(data)

          :sint64 ->
            Protox.Decode.parse_sint64(data)

          :bool ->
            Protox.Decode.parse_bool(data)

          :string ->
            {len, rest} = Protox.Varint.decode(data)
            <<string::binary-size(len), rest::binary>> = rest
            {string, rest}

          :bytes ->
            {len, rest} = Protox.Varint.decode(data)
            <<string::binary-size(len), rest::binary>> = rest
            {Base.encode64(string), rest}

          :enum ->
            {number, rest} = Protox.Decode.parse_uint32(data)
            {schema.enums[type_name].values[number], rest}

          :message ->
            {len, rest} = Protox.Varint.decode(data)
            <<to_decode::binary-size(len), rest::binary>> = rest
            {:ok, data} = decode(schema, type_name, to_decode)
            {data, rest}
        end

      {:ok, result}
    rescue
      e -> {:error, e}
    end
  end
end
