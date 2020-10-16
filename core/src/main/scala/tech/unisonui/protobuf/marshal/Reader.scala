package tech.unisonui.protobuf.marshal

import java.io.InputStream
import java.{util => ju}

import cats.implicits._
import com.google.protobuf.Descriptors.FieldDescriptor
import com.google.protobuf.Descriptors.FieldDescriptor.Type
import io.circe.Json
import io.circe.syntax._
import tech.unisonui.protobuf.data._
import tech.unisonui.protobuf.json._

import scala.util.chaining._

object Reader {
  implicit class ReaderOps(private val schema: Schema) {
    private val reader                                  = new Reader(schema)
    def read(buf: InputStream): Either[Throwable, Json] = reader.read(buf)
  }

}

class Reader(private val schema: Schema) {
  def read(input: InputStream): Either[Throwable, Json] =
    read(SafeCodedInputStream(input), schema.root)

  private def read(
      input: SafeCodedInputStream,
      messageSchema: Option[MessageSchema]): Either[Throwable, Json] =
    messageSchema match {
      case Some(messageSchema) =>
        for {
          input <- decodeInput(input, messageSchema)
          result <- input.toVector.traverse { case (key, json) =>
            json.map(key -> _)
          }
          defaults = messageSchema.fields.valuesIterator.collect {
            case field @ Field(_, name, _, _, _, maybeDefault, _, _)
                if !result.exists(_._1 == name) =>
              name -> maybeDefault.fold(setDefault(field))(
                _.asInstanceOf[Any].asJson)
          }
        } yield Json.obj(result ++ defaults: _*)
      case None => Json.Null.asRight
    }

  private def setDefault(field: Field): Json =
    if (field.label == Label.Repeated && !field.schema
        .flatMap(schema.messages.get)
        .exists(_.isMap)) Json.arr()
    else
// $COVERAGE-OFF$
      field.`type` match {
        case FieldDescriptor.Type.BOOL => Json.fromBoolean(false)
        case FieldDescriptor.Type.FIXED32 | FieldDescriptor.Type.SFIXED32 |
            FieldDescriptor.Type.SINT32 | FieldDescriptor.Type.INT32 |
            FieldDescriptor.Type.FIXED64 | FieldDescriptor.Type.SFIXED64 |
            FieldDescriptor.Type.SINT64 | FieldDescriptor.Type.INT64 =>
          Json.fromInt(0)
        case FieldDescriptor.Type.FLOAT | FieldDescriptor.Type.DOUBLE =>
          Json.fromDoubleOrNull(0.0)
        case FieldDescriptor.Type.STRING | FieldDescriptor.Type.BYTES =>
          Json.fromString("")
        case FieldDescriptor.Type.ENUM =>
          Json.fromString(schema.enums(field.schema.get).values(1))
        case _ => Json.Null
      }
// $COVERAGE-ON$

  private def decodeInput(input: SafeCodedInputStream,
                          messageSchema: MessageSchema,
                          fields: Map[String, Either[Throwable, Json]] =
                            Map.empty)
      : Either[Throwable, Map[String, Either[Throwable, Json]]] =
    input.isAtEnd.flatMap {
      case true => fields.asRight[Throwable]
      case false =>
        for {
          id           <- input.readFieldId
          fieldOrOneOf <- findField(id, messageSchema)
          name = fieldOrOneOf.fold(_._1, _._1)
          value = fieldOrOneOf match {
            case Left((_, field)) =>
              field.pipe(readValue(input, _)).map { value =>
                Json.obj("type"  -> Json.fromString(field.name),
                         "value" -> value)
              }
            case Right(
                  (name, field @ Field(_, _, Label.Repeated, _, _, _, _, _))) =>
              fields
                .get(name)
                .toVector
                .flatTraverse(_.map { json =>
                  if (json.isObject) Vector(json)
                  else json.asArray.toVector.flatten
                })
                .pipe(decodeRepeat(input, field, _))
            case Right((_, field)) => readValue(input, field)
          }
          result <- decodeInput(input, messageSchema, fields + (name -> value))
        } yield result
    }

  private def findField(id: Int, messageSchema: MessageSchema)
      : Either[Throwable, Either[(String, Field), (String, Field)]] =
    messageSchema.fields
      .get(id)
      .map(field => (field.name -> field).asRight[(String, Field)])
      .fold {
        messageSchema.oneOfs.collectFirst {
          case (name, fields) if fields.contains(id) => name -> fields(id)
        }.map(_.asLeft[(String, Field)])
          .toRight[Throwable](Errors.InvalidField(id))
      }(_.asRight[Throwable])

  private def decodeRepeat(
      input: SafeCodedInputStream,
      field: Field,
      maybeList: Either[Throwable, Vector[Json]]): Either[Throwable, Json] =
    decodeList(input, field, maybeList)
      .flatMap(_.sequence.map { list =>
        field.schema.flatMap(schema.messages.get(_)) match {
          case Some(subSchema) if subSchema.isMap =>
            list
              .foldRight(Vector.empty[(String, Json)]) {
                case (json, acc) if acc.isEmpty =>
                  val keyValues =
                    json.asObject.toVector.flatMap(_.toVector).toMap
                  (for {
                    key   <- keyValues("key").asString
                    value <- keyValues.get("value")
                  } yield (key -> value)).fold(acc)(obj => acc :+ obj)
                case (json, acc) =>
                  acc ++ json.asObject.toVector.flatMap(_.toVector)
              }
              .reverse
              .pipe(Json.obj(_: _*))
          case _ => Json.arr(list: _*)
        }
      })

  private def decodeList(input: SafeCodedInputStream,
                         field: Field,
                         maybeList: Either[Throwable, Vector[Json]])
      : Either[Throwable, Vector[Either[Throwable, Json]]] =
    if (field.packed)
      input.readByteBuffer
        .flatMap(
          SafeCodedInputStream(_).pipe(
            decodeList(_, field, convertEitherToVector(maybeList))))
    else
      (for {
        list  <- maybeList
        value <- readValue(input, field)
      } yield list :+ value).pipe(convertEitherToVector).asRight[Throwable]

  private def convertEitherToVector(maybeList: Either[Throwable, Vector[Json]])
      : Vector[Either[Throwable, Json]] =
    maybeList.foldMap(_.map(_.asRight[Throwable]))

  private def decodeList(input: SafeCodedInputStream,
                         field: Field,
                         list: Vector[Either[Throwable, Json]])
      : Either[Throwable, Vector[Either[Throwable, Json]]] =
    input.isAtEnd.flatMap {
      case true  => list.asRight[Throwable]
      case false => decodeList(input, field, list :+ readValue(input, field))
    }

  private def readValue(in: SafeCodedInputStream,
                        field: Field): Either[Throwable, Json] =
    field.`type` match {
// $COVERAGE-OFF$
      case Type.FLOAT    => in.readFloat.map(Json.fromFloatOrNull)
      case Type.DOUBLE   => in.readDouble.map(Json.fromDoubleOrNull)
      case Type.FIXED32  => in.readFixed32.map(Json.fromInt)
      case Type.FIXED64  => in.readFixed64.map(Json.fromLong)
      case Type.INT32    => in.readInt32.map(Json.fromInt)
      case Type.INT64    => in.readInt64.map(Json.fromLong)
      case Type.UINT32   => in.readUInt32.map(Json.fromInt)
      case Type.UINT64   => in.readUInt64.map(Json.fromLong)
      case Type.SFIXED32 => in.readSFixed32.map(Json.fromInt)
      case Type.SFIXED64 => in.readSFixed64.map(Json.fromLong)
      case Type.SINT32   => in.readSInt32.map(Json.fromInt)
      case Type.SINT64   => in.readSInt64.map(Json.fromLong)
      case Type.BOOL     => in.readBool.map(Json.fromBoolean)
// $COVERAGE-ON$
      case Type.STRING => in.readString.map(Json.fromString)
      case Type.BYTES =>
        in.readByteArray.map(
          _.pipe(ju.Base64.getEncoder.encode)
            .pipe(new String(_))
            .pipe(Json.fromString))
      case Type.ENUM =>
        for {
          id <- in.readEnum
          result <- schema
            .enums(field.schema.get)
            .values
            .get(id)
            .toRight(Errors.InvalidEnumEntry(id))
        } yield Json.fromString(result)
      case Type.MESSAGE =>
        in.readByteBuffer.flatMap(
          _.pipe(SafeCodedInputStream(_)).pipe(
            read(_, schema.messages(field.schema.get).some)))
      case Type.GROUP =>
        (new IllegalArgumentException("Unsupported type: GROUP")).asLeft
    }
}
