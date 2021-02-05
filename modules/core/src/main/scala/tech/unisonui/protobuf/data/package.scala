package tech.unisonui.protobuf

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
      options: Option[Map[String, String]] = None,
      oneOfs: Map[String, Map[Int, Field]] = Map.empty
  ) extends DescriptorSchema {
    val isMap: Boolean =
      options.exists {
        _.exists {
          case ("google.protobuf.MessageOptions.map_entry", "true") => true
          case _                                                    => false
        }
      }
  }

  object MessageSchema {
    implicit val encoder: Encoder[MessageSchema] = (message: MessageSchema) =>
      Json
        .obj(
          "name"   -> Json.fromString(message.name),
          "fields" -> message.fields.values.toVector.sortBy(_.id).asJson,
          "oneOf" -> message.oneOfs.view
            .mapValues(_.values.toVector.sortBy(_.id))
            .toMap
            .asJson,
          "options" -> message.options.asJson
        )
        .dropNullValues
  }

  final case class EnumSchema(name: String,
                              values: Map[Int, String],
                              options: Option[Map[String, String]] = None)
      extends DescriptorSchema

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
      default: Option[AnyRef] = None,
      schema: Option[String] = None,
      options: Option[Map[String, String]] = None
  )

  object Field {
    import tech.unisonui.protobuf.json.{encoder => anyEncoder}
    implicit val encoder: Encoder[Field] = (field: Field) =>
      Json
        .obj(
          "id"      -> Json.fromInt(field.id),
          "name"    -> Json.fromString(field.name),
          "label"   -> field.label.asJson,
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
    implicit val typeEncoder: Encoder[FieldDescriptor.Type] =
      (fieldType: FieldDescriptor.Type) => Json.fromString(fieldType.name())
  }

  final case class Service(name: String,
                           fullName: String,
                           methods: Vector[Method])
  final case class Method(name: String,
                          inputType: Schema,
                          outputType: Schema,
                          isServerStreaming: Boolean,
                          isClientStreaming: Boolean)

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
        "name" -> Json.fromString(method.name),
        "streaming" -> Json.obj(
          "server" -> Json.fromBoolean(method.isServerStreaming),
          "client" -> Json.fromBoolean(method.isClientStreaming)),
        "inputType"  -> Json.fromString(method.inputType.rootKey.get),
        "outputType" -> Json.fromString(method.outputType.rootKey.get)
      )
  }
}
