defmodule GRPC.Protobuf.Descriptor do
  @moduledoc false
  # Transcription of descriptor.proto.
  # https://raw.githubusercontent.com/google/protobuf/master/src/google/protobuf/descriptor.proto

  use Protox.Define,
    enums: [
      {
        GRPC.Protobuf.FieldDescriptorProto.Type,
        [
          {1, :double},
          {2, :float},
          {3, :int64},
          {4, :uint64},
          {5, :int32},
          {6, :fixed64},
          {7, :fixed32},
          {8, :bool},
          {9, :string},
          {10, :group},
          {11, :message},
          {12, :bytes},
          {13, :uint32},
          {14, :enum},
          {15, :sfixed32},
          {16, :sfixed64},
          {17, :sint32},
          {18, :sint64}
        ]
      },
      {
        GRPC.Protobuf.FieldDescriptorProto.Label,
        [
          {1, :optional},
          {2, :required},
          {3, :repeated}
        ]
      }
    ],
    messages: [
      {
        GRPC.Protobuf.FileDescriptorSet,
        :proto3,
        [
          Protox.Field.new!(
            tag: 1,
            label: :repeated,
            name: :file,
            kind: :unpacked,
            type: {:message, GRPC.Protobuf.FileDescriptorProto}
          )
        ]
      },
      {
        GRPC.Protobuf.FileDescriptorProto,
        :proto3,
        [
          Protox.Field.new!(
            tag: 1,
            label: :none,
            name: :name,
            kind: {:scalar, ""},
            type: :string
          ),
          Protox.Field.new!(
            tag: 2,
            label: :none,
            name: :package,
            kind: {:scalar, ""},
            type: :string
          ),
          Protox.Field.new!(
            tag: 3,
            label: :repeated,
            name: :dependency,
            kind: :unpacked,
            type: :string
          ),
          Protox.Field.new!(
            tag: 4,
            label: :repeated,
            name: :message_type,
            kind: :unpacked,
            type: {:message, GRPC.Protobuf.DescriptorProto}
          ),
          Protox.Field.new!(
            tag: 5,
            label: :repeated,
            name: :enum_type,
            kind: :unpacked,
            type: {:message, GRPC.Protobuf.EnumDescriptorProto}
          ),
          Protox.Field.new!(
            tag: 6,
            label: :repeated,
            name: :service,
            kind: :unpacked,
            type: {:message, GRPC.Protobuf.ServiceDescriptorProto}
          ),
          Protox.Field.new!(
            tag: 7,
            label: :repeated,
            name: :extension,
            kind: :unpacked,
            type: {:message, GRPC.Protobuf.FieldDescriptorProto}
          ),
          Protox.Field.new!(
            tag: 12,
            label: :none,
            name: :syntax,
            kind: {:scalar, ""},
            type: :string
          )
        ]
      },
      {
        GRPC.Protobuf.DescriptorProto.ExtensionRange,
        :proto3,
        [
          Protox.Field.new!(
            tag: 1,
            label: :none,
            name: :start,
            kind: {:scalar, 0},
            type: :int32
          ),
          Protox.Field.new!(tag: 2, label: :none, name: :end, kind: {:scalar, 0}, type: :int32)
        ]
      },
      # GRPC.Protobuf.DescriptorProto.ReservedRange ignored
      {
        GRPC.Protobuf.DescriptorProto,
        :proto3,
        [
          # Ignored: 9, 10
          Protox.Field.new!(
            tag: 1,
            label: :none,
            name: :name,
            kind: {:scalar, nil},
            type: :string
          ),
          Protox.Field.new!(
            tag: 2,
            label: :repeated,
            name: :field,
            kind: :unpacked,
            type: {:message, GRPC.Protobuf.FieldDescriptorProto}
          ),
          Protox.Field.new!(
            tag: 6,
            label: :repeated,
            name: :extension,
            kind: :unpacked,
            type: {:message, GRPC.Protobuf.FieldDescriptorProto}
          ),
          Protox.Field.new!(
            tag: 3,
            label: :repeated,
            name: :nested_type,
            kind: :unpacked,
            type: {:message, GRPC.Protobuf.DescriptorProto}
          ),
          Protox.Field.new!(
            tag: 4,
            label: :repeated,
            name: :enum_type,
            kind: :unpacked,
            type: {:message, GRPC.Protobuf.EnumDescriptorProto}
          ),
          Protox.Field.new!(
            tag: 5,
            label: :repeated,
            name: :extension_range,
            kind: :unpacked,
            type: {:message, GRPC.Protobuf.DescriptorProto.ExtensionRange}
          ),
          Protox.Field.new!(
            tag: 8,
            label: :repeated,
            name: :oneof_decl,
            kind: :unpacked,
            type: {:message, GRPC.Protobuf.OneofDescriptorProto}
          ),
          Protox.Field.new!(
            tag: 7,
            label: :none,
            name: :options,
            kind: {:scalar, nil},
            type: {:message, GRPC.Protobuf.MessageOptions}
          )
        ]
      },
      {
        GRPC.Protobuf.FieldDescriptorProto,
        :proto3,
        [
          # Ignored: 10
          Protox.Field.new!(
            tag: 1,
            label: :none,
            name: :name,
            kind: {:scalar, nil},
            type: :string
          ),
          Protox.Field.new!(
            tag: 3,
            label: :none,
            name: :number,
            kind: {:scalar, nil},
            type: :int32
          ),
          Protox.Field.new!(
            tag: 4,
            label: :none,
            name: :label,
            kind: {:scalar, nil},
            type: {:enum, GRPC.Protobuf.FieldDescriptorProto.Label}
          ),
          Protox.Field.new!(
            tag: 5,
            label: :none,
            name: :type,
            kind: {:scalar, nil},
            type: {:enum, GRPC.Protobuf.FieldDescriptorProto.Type}
          ),
          Protox.Field.new!(
            tag: 6,
            label: :none,
            name: :type_name,
            kind: {:scalar, nil},
            type: :string
          ),
          Protox.Field.new!(
            tag: 2,
            label: :none,
            name: :extendee,
            kind: {:scalar, nil},
            type: :string
          ),
          Protox.Field.new!(
            tag: 7,
            label: :none,
            name: :default_value,
            kind: {:scalar, nil},
            type: :string
          ),
          Protox.Field.new!(
            tag: 9,
            label: :none,
            name: :oneof_index,
            kind: {:scalar, nil},
            type: :int32
          ),
          Protox.Field.new!(
            tag: 8,
            label: :none,
            name: :options,
            kind: {:scalar, nil},
            type: {:message, GRPC.Protobuf.FieldOptions}
          )
        ]
      },
      {
        GRPC.Protobuf.OneofDescriptorProto,
        :proto3,
        [
          # Ignored: 2
          Protox.Field.new!(
            tag: 1,
            label: :none,
            name: :name,
            kind: {:scalar, nil},
            type: :string
          )
        ]
      },
      {
        GRPC.Protobuf.EnumDescriptorProto,
        :proto3,
        [
          # Ignored: 3
          Protox.Field.new!(
            tag: 1,
            label: :none,
            name: :name,
            kind: {:scalar, nil},
            type: :string
          ),
          Protox.Field.new!(
            tag: 2,
            label: :repeated,
            name: :value,
            kind: :unpacked,
            type: {:message, GRPC.Protobuf.EnumValueDescriptorProto}
          )
        ]
      },
      {
        GRPC.Protobuf.EnumValueDescriptorProto,
        :proto3,
        [
          # Ignored: 3
          Protox.Field.new!(
            tag: 1,
            label: :none,
            name: :name,
            kind: {:scalar, nil},
            type: :string
          ),
          Protox.Field.new!(
            tag: 2,
            label: :none,
            name: :number,
            kind: {:scalar, nil},
            type: :int32
          )
        ]
      },
      # ServiceDescriptorProto ignored
      # MethodDescriptorProto ignored
      # FileOptions ignored
      {
        GRPC.Protobuf.MessageOptions,
        :proto3,
        [
          # 1, 2, 999 ignored
          Protox.Field.new!(
            tag: 3,
            label: :none,
            name: :deprecated,
            kind: {:scalar, false},
            type: :bool
          ),
          Protox.Field.new!(
            tag: 7,
            label: :none,
            name: :map_entry,
            kind: {:scalar, false},
            type: :bool
          )
        ]
      },
      {
        GRPC.Protobuf.FieldOptions,
        :proto3,
        [
          # 1, 6, 5, 10, 999 ignored
          Protox.Field.new!(
            tag: 2,
            label: :none,
            name: :packed,
            kind: {:scalar, nil},
            type: :bool
          ),
          Protox.Field.new!(
            tag: 3,
            label: :none,
            name: :deprecated,
            kind: {:scalar, false},
            type: :bool
          )
        ]
      },
      {
        GRPC.Protobuf.ServiceDescriptorProto,
        :proto3,
        [
          # 1, 6, 5, 10, 999 ignored
          Protox.Field.new!(
            tag: 1,
            label: :none,
            name: :name,
            kind: {:scalar, nil},
            type: :string
          ),
          Protox.Field.new!(
            tag: 2,
            label: :repeated,
            name: :method,
            kind: :unpacked,
            type: {:message, GRPC.Protobuf.MethodDescriptorProto}
          ),
          Protox.Field.new!(
            tag: 3,
            label: :none,
            name: :option,
            kind: :unpacked,
            type: {:message, GRPC.Protobuf.ServiceOptions}
          )
        ]
      },
      {
        GRPC.Protobuf.MethodDescriptorProto,
        :proto3,
        [
          Protox.Field.new!(
            tag: 1,
            label: :none,
            name: :name,
            kind: {:scalar, nil},
            type: :string
          ),
          Protox.Field.new!(
            tag: 2,
            label: :none,
            name: :input_type,
            kind: {:scalar, nil},
            type: :string
          ),
          Protox.Field.new!(
            tag: 3,
            label: :none,
            name: :output_type,
            kind: {:scalar, nil},
            type: :string
          ),
          Protox.Field.new!(
            tag: 4,
            label: :repeated,
            name: :options,
            kind: :unpacked,
            type: {:message, GRPC.Protobuf.MethodOptions}
          ),
          Protox.Field.new!(
            tag: 5,
            label: :none,
            name: :client_streaming,
            kind: {:scalar, false},
            type: :bool
          ),
          Protox.Field.new!(
            tag: 6,
            label: :none,
            name: :server_streaming,
            kind: {:scalar, false},
            type: :bool
          )
        ]
      },
      {
        GRPC.Protobuf.ServiceOptions,
        :proto3,
        [
          Protox.Field.new!(
            tag: 33,
            label: :none,
            name: :deprecated,
            kind: {:scalar, false},
            type: :bool
          )
        ]
      },
      {
        GRPC.Protobuf.MethodOptions,
        :proto3,
        [
          Protox.Field.new!(
            tag: 33,
            label: :none,
            name: :deprecated,
            kind: {:scalar, false},
            type: :bool
          )
        ]
      }

      # OneofOptions ignored
      # EnumOptions ignored
      # EnumValueOptions ignored
      # ServiceOptions ignored
      # MethodOptions ignored
      # UninterpretedOption ignored
      # SourceCodeInfo ignored
      # GeneratedCodeInfo ignored
    ]
end
