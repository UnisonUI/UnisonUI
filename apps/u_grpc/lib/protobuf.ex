defmodule GRPC.Protobuf do
  require OK
  alias GRPC.Protobuf.Structs.{Schema, MessageSchema, EnumSchema, Field, Service, Method}

  def compile(file) do
    file = Path.expand(file)

    OK.for do
      _ <- File.stat(file)
      protoset <- run_protoc(file)

      %Protox.Google.Protobuf.FileDescriptorSet{file: files} <-
        Protox.Google.Protobuf.FileDescriptorSet.decode(protoset)

      schema = to_schema(files)
    after
      schema
    end
  end

  def from_file_descriptors(file_descriptors) do
    files =
      Enum.reduce_while(file_descriptors, {:ok, []}, fn bytes, {_, files} ->
        case Protox.Google.Protobuf.FileDescriptorProto.decode(bytes) do
          {:ok, file_descriptor} -> {:cont, {:ok, [file_descriptor | files]}}
          error -> {:halt, error}
        end
      end)

    case files do
      {:ok, files} -> OK.success(to_schema(files))
      error -> error
    end
  end

  def compile!(file) do
    case compile(file) do
      {:ok, schema} -> schema
      {:error, ex} -> raise ex
    end
  end

  defp run_protoc(proto_file) do
    tmp_dir = System.tmp_dir!()
    include_path = proto_file |> Path.dirname() |> Path.expand()

    file =
      "unisonui_" <>
        (:crypto.strong_rand_bytes(16) |> Base.encode32(case: :lower, padding: false)) <>
        ".protoset"

    outfile_path = Path.join(tmp_dir, file)

    cmd_args = ["--include_imports", "-o", outfile_path, "-I", include_path, proto_file]

    try do
      System.cmd("protoc", cmd_args, stderr_to_stdout: true)
    catch
      :error, :enoent ->
        msg =
          "protoc executable is missing. Please make sure Protocol Buffers " <>
            "is installed and available system wide"

        {:error, GRPC.Protobuf.ProtocError.exception(msg)}
    else
      {_, 0} ->
        file_content = File.read!(outfile_path)
        _ = File.rm(outfile_path)
        {:ok, file_content}

      {msg, _} ->
        msg = String.trim(msg)
        {:error, GRPC.Protobuf.ProtocError.exception(msg)}
    end
  end

  @spec decode(
          schema :: GRPC.Protobuf.Structs.Schema.t(),
          type_name :: String.t(),
          data :: binary()
        ) :: {:ok, map()} | {:error, term()}
  def decode(schema, type_name, data), do: GRPC.Protobuf.Serde.decode(schema, type_name, data)

  @spec encode(
          schema :: GRPC.Protobuf.Structs.Schema.t(),
          type_name :: String.t(),
          data :: map()
        ) :: {:ok, binary()} | {:error, term()}
  def encode(schema, type_name, data), do: GRPC.Protobuf.Serde.encode(schema, type_name, data)

  defp to_schema(file_descriptors, schema \\ %Schema{})
  defp to_schema([], schema), do: schema

  defp to_schema(
         [
           %{
             message_type: message_type,
             package: package,
             enum_type: enum_type,
             service: services,
             syntax: syntax
           }
           | tail
         ],
         %Schema{messages: current_messages, enums: current_enums, services: current_services}
       ) do
    {messages, enums} = decode_descriptors(message_type, package, syntax, {%{}, %{}})
    enums = Map.merge(enums, decode_descriptors(enum_type, package, syntax, %{}))
    services = decode_descriptors(services, package, syntax, %{})

    schema = %Schema{
      messages: Map.merge(current_messages, messages),
      enums: Map.merge(current_enums, enums),
      services: Map.merge(current_services, services)
    }

    to_schema(tail, schema)
  end

  defp scalar_packed(:double, "proto3"), do: true
  defp scalar_packed(:float, "proto3"), do: true
  defp scalar_packed(:int64, "proto3"), do: true
  defp scalar_packed(:uint64, "proto3"), do: true
  defp scalar_packed(:int32, "proto3"), do: true
  defp scalar_packed(:fixed64, "proto3"), do: true
  defp scalar_packed(:fixed32, "proto3"), do: true
  defp scalar_packed(:uint32, "proto3"), do: true
  defp scalar_packed(:sfixed32, "proto3"), do: true
  defp scalar_packed(:sfixed64, "proto3"), do: true
  defp scalar_packed(:sint32, "proto3"), do: true
  defp scalar_packed(:sint64, "proto3"), do: true
  defp scalar_packed(_, _), do: false

  defp decode_descriptors([], _package, _syntax, result), do: result

  defp decode_descriptors(
         [%Protox.Google.Protobuf.ServiceDescriptorProto{method: method, name: name} | tail],
         package,
         syntax,
         result
       ) do
    full_name = full_name(package, name)

    method =
      method
      |> Enum.map(fn %{
                       client_streaming: client_streaming,
                       server_streaming: server_streaming,
                       name: name,
                       input_type: input_type,
                       output_type: output_type
                     } ->
        %Method{
          name: name,
          input_type: String.trim_leading(input_type, "."),
          output_type: String.trim_leading(output_type, "."),
          server_streaming?: server_streaming,
          client_streaming?: client_streaming
        }
      end)

    result =
      Map.put(result, full_name, %Service{name: name, full_name: full_name, methods: method})

    decode_descriptors(tail, package, syntax, result)
  end

  defp decode_descriptors(
         [%Protox.Google.Protobuf.EnumDescriptorProto{value: values, name: name} | tail],
         package,
         syntax,
         result
       ) do
    full_name = full_name(package, name)

    values =
      values |> Enum.map(fn %{name: name, number: number} -> {number, name} end) |> Enum.into(%{})

    enum = %EnumSchema{name: full_name, values: values}
    result = Map.put(result, full_name, enum)
    decode_descriptors(tail, package, syntax, result)
  end

  defp decode_descriptors(
         [
           %Protox.Google.Protobuf.DescriptorProto{
             field: fields,
             name: name,
             options: options,
             enum_type: enum_type,
             nested_type: nested_type
           } = descriptor
           | tail
         ],
         package,
         syntax,
         {schemas, enums}
       ) do
    one_ofs =
      descriptor
      |> Map.get(:oneof_decl, [])
      |> Stream.with_index()
      |> Stream.flat_map(fn {%{name: name}, oneof_index} ->
        fields
        |> Stream.filter(&match?(%{oneof_index: ^oneof_index}, &1))
        |> Enum.map(fn %{number: field_number} -> {field_number, name} end)
      end)
      |> Enum.into(%{})

    {fields, oneofs} =
      Enum.reduce(fields, {%{}, %{}}, fn %{
                                           number: number,
                                           name: name,
                                           default_value: default_value,
                                           type_name: type_name,
                                           type: type,
                                           options: options,
                                           label: label
                                         },
                                         {fields, oneofs} ->
        default_value =
          case {default_value, type} do
            {nil, _} ->
              nil

            {value, :bytes} ->
              Base.encode64(value)

            {value, :enum} ->
              value

            _ ->
              nil
          end

        field = %Field{
          id: number,
          name: name,
          label: label,
          type: type,
          packed: (options || %{}) |> Map.get(:packed, scalar_packed(type, syntax)),
          default: default_value,
          schema: type_name && String.trim_leading(type_name, "."),
          options: decode_options(options)
        }

        fields = Map.put(fields, number, field)

        oneofs =
          case Map.get(one_ofs, number) do
            nil ->
              oneofs

            one_of_name ->
              Map.update(oneofs, one_of_name, %{number => field}, fn map ->
                Map.put(map, number, field)
              end)
          end

        {Map.put(fields, number, field), oneofs}
      end)

    full_name = full_name(package, name)

    schemas =
      Map.put(schemas, full_name, %MessageSchema{
        name: full_name,
        fields: fields,
        options: decode_options(options),
        one_ofs: oneofs
      })

    {schemas, enums} = decode_descriptors(nested_type, full_name, syntax, {schemas, enums})

    decode_descriptors(
      tail,
      package,
      syntax,
      {schemas, Map.merge(enums, decode_descriptors(enum_type, package, syntax, %{}))}
    )
  end

  defp full_name("", name), do: name
  defp full_name(package, name), do: "#{package}.#{name}"
  defp decode_options(nil), do: %{}

  defp decode_options(struct),
    do:
      struct
      |> Map.drop([:__struct__, :__uf__])
      |> Enum.map(fn {k, v} -> {to_string(k), to_string(v)} end)
      |> Enum.into(%{})
end
