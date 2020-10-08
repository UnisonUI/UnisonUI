package restui.protobuf

import com.google.protobuf.Descriptors.FieldDescriptor
import io.circe.syntax._
import io.circe.{Encoder, Json}

package object data {
  sealed trait Label

  object Label {
    case object Required extends Label
    case object Optional extends Label
    case object Repeated extends Label
    implicit val encoder: Encoder[Label] = Encoder.instance {
      case Required => Json.fromString("required")
      case Optional => Json.fromString("optional")
      case Repeated => Json.fromString("repeated")
    }
  }

  sealed trait DescriptorSchema

  final case class MessageSchema(
      name: String,
      fields: Map[Int, Field],
      options: Option[Map[String, String]]
  ) extends DescriptorSchema

  object MessageSchema {
    implicit val encoder: Encoder[MessageSchema] = (message: MessageSchema) =>
      Json
        .obj(
          "name"    -> Json.fromString(message.name),
          "fields"  -> message.fields.values.asJson,
          "options" -> message.options.asJson
        )
        .dropNullValues
  }

  final case class EnumSchema(name: String, values: Map[Int, String], options: Option[Map[String, String]]) extends DescriptorSchema

  object EnumSchema {
    implicit val encoder: Encoder[EnumSchema] = (message: EnumSchema) =>
      Json
        .obj(
          "name"    -> Json.fromString(message.name),
          "values"  -> message.values.asJson,
          "options" -> message.options.asJson
        )
        .dropNullValues
  }

  final case class Field(
      id: Int,
      name: String,
      label: Label,
      `type`: FieldDescriptor.Type,
      packed: Boolean,
      default: Option[AnyRef],
      schema: Option[String],
      options: Option[Map[String, String]]
  )

  object Field {
    import restui.protobuf.json.{encoder => anyEncoder}
    implicit val encoder: Encoder[Field] = (field: Field) =>
      Json
        .obj(
          "id"      -> Json.fromInt(field.id),
          "name"    -> Json.fromString(field.name),
          "name"    -> field.label.asJson,
          "type"    -> field.`type`.asJson,
          "packed"  -> Json.fromBoolean(field.packed),
          "default" -> field.default.asJson,
          "schema"  -> field.schema.asJson,
          "options" -> field.options.asJson
        )
        .dropNullValues

    implicit val optionAny: Encoder[Option[AnyRef]] = Encoder.instance {
      case None        => Json.Null
      case Some(value) => value.asInstanceOf[Any].asJson
    }
    implicit val typeEncoder: Encoder[FieldDescriptor.Type] = Encoder.instance {
      case FieldDescriptor.Type.BOOL     => Json.fromString("bool")
      case FieldDescriptor.Type.BYTES    => Json.fromString("bytes")
      case FieldDescriptor.Type.DOUBLE   => Json.fromString("double")
      case FieldDescriptor.Type.ENUM     => Json.fromString("enum")
      case FieldDescriptor.Type.FIXED32  => Json.fromString("fixed32")
      case FieldDescriptor.Type.FIXED64  => Json.fromString("fixed64")
      case FieldDescriptor.Type.FLOAT    => Json.fromString("float")
      case FieldDescriptor.Type.INT32    => Json.fromString("int32")
      case FieldDescriptor.Type.INT64    => Json.fromString("int64")
      case FieldDescriptor.Type.MESSAGE  => Json.fromString("message")
      case FieldDescriptor.Type.SFIXED32 => Json.fromString("sfixed32")
      case FieldDescriptor.Type.SFIXED64 => Json.fromString("sfixed64")
      case FieldDescriptor.Type.SINT32   => Json.fromString("sint32")
      case FieldDescriptor.Type.SINT64   => Json.fromString("sint64")
      case FieldDescriptor.Type.STRING   => Json.fromString("string")
      case FieldDescriptor.Type.UINT32   => Json.fromString("uint32")
      case FieldDescriptor.Type.UINT64   => Json.fromString("uint64")
      case FieldDescriptor.Type.GROUP    => Json.fromString("group")
    }
  }

  final case class Service(name: String, fullName: String, methods: List[Method])
  final case class Method(name: String, inputType: Schema, outputType: Schema)
  object Service {
    implicit val encoder: Encoder[Service] = (service: Service) =>
      Json.obj(
        "name"     -> Json.fromString(service.name),
        "fullName" -> Json.fromString(service.fullName),
        "methods"  -> service.methods.asJson
      )
  }
  object Method {
    implicit val encoder: Encoder[Method] = (method: Method) =>
      Json.obj(
        "name"       -> Json.fromString(method.name),
        "inputType"  -> Json.fromString(method.inputType.rootKey.get),
        "outputType" -> Json.fromString(method.outputType.rootKey.get)
      )
  }
}
