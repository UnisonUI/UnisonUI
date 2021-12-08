defmodule GRPC.Protobuf.Structs do
  defmodule Schema do
    @type t :: %__MODULE__{
            messages: %{String.t() => GRPC.Protobuf.Structs.MessageSchema.t()},
            enums: %{String.t() => GRPC.Protobuf.Structs.EnumSchema.t()},
            services: %{String.t() => GRPC.Protobuf.Structs.Service.t()}
          }
    defstruct messages: %{}, enums: %{}, services: %{}
  end

  defmodule MessageSchema do
    @type fields :: %{pos_integer() => GRPC.Protobuf.Structs.Field.t()}
    @type t :: %__MODULE__{
            name: String.t(),
            fields: fields(),
            options: %{String.t() => String.t()} | nil,
            one_ofs: %{String.t() => fields()}
          }
    defstruct [:name, :fields, :options, :one_ofs]
  end

  defmodule EnumSchema do
    @type t :: %__MODULE__{
            name: String.t(),
            values: %{pos_integer() => String.t()}
          }
    defstruct [:name, :values]
  end

  defmodule Field do
    @type t :: %__MODULE__{
            id: pos_integer(),
            name: String.t(),
            label: :required | :optional | :repeated,
            type: :atom,
            packed: boolean(),
            default: any(),
            schema: String.t() | nil,
            options: %{String.t() => String.t()} | nil
          }
    defstruct [:id, :name, :label, :type, :packed, :default, :schema, :options]
  end

  defmodule Service do
    @type t :: %__MODULE__{
            name: String.t(),
            full_name: String.t(),
            methods: [GRPC.Protobuf.Structs.Method.t()]
          }
    defstruct [:name, :full_name, :methods]
  end

  defmodule Method do
    @type t :: %__MODULE__{
            name: String.t(),
            input_type: String.t(),
            output_type: String.t(),
            server_streaming?: boolean(),
            client_streaming?: boolean()
          }
    defstruct [:name, :input_type, :output_type, :server_streaming?, :client_streaming?]
  end

  defimpl Jason.Encoder, for: [Schema, MessageSchema, EnumSchema, Field, Service, Method] do
    alias GRPC.Protobuf.Structs.{MessageSchema, Field, Method, Schema}

    def encode(
          %MessageSchema{name: name, fields: fields, options: options, one_ofs: one_ofs},
          opts
        ),
        do:
          %{
            name: name,
            fields: map_to_list(fields) |> Enum.sort_by(& &1.id, :asc),
            oneOf:
              one_ofs
              |> Enum.into(%{}, fn {key, value} ->
                value = map_to_list(value) |> Enum.sort_by(& &1.id, :asc)
                {key, value}
              end),
            options: options
          }
          |> Enum.reject(&is_nil(elem(&1, 1)))
          |> Enum.into(%{})
          |> Jason.Encode.value(opts)

    def encode(
          %Method{
            name: name,
            input_type: input_type,
            output_type: output_type,
            server_streaming?: server_streaming?,
            client_streaming?: client_streaming?
          },
          opts
        ),
        do:
          %{
            name: name,
            streaming: %{server: server_streaming?, client: client_streaming?},
            inputType: input_type,
            outputType: output_type
          }
          |> Jason.Encode.value(opts)

    def encode(%Field{} = field, opts),
      do:
        field
        |> Map.from_struct()
        |> Map.update!(:type, fn type -> type |> Atom.to_string() |> String.upcase() end)
        |> Enum.reject(&is_nil(elem(&1, 1)))
        |> Enum.into(%{})
        |> Jason.Encode.value(opts)

    def encode(%Schema{messages: messages, enums: enums, services: services}, opts),
      do:
        %{
          messages: map_to_list(messages),
          enums: map_to_list(enums),
          services: map_to_list(services)
        }
        |> Jason.Encode.value(opts)

    def encode(struct, opts),
      do:
        struct
        |> Map.from_struct()
        |> Enum.reject(&is_nil(elem(&1, 1)))
        |> Enum.into(%{})
        |> Jason.Encode.value(opts)

    defp map_to_list(map), do: Enum.into(map, [], &elem(&1, 1))
  end
end
