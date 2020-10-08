package restui.protobuf.marshal
import java.io.InputStream
import java.{util => ju}

import scala.annotation.tailrec
import scala.util.chaining._

import cats.implicits._
import com.google.protobuf.Descriptors.FieldDescriptor
import com.google.protobuf.Descriptors.FieldDescriptor.Type
import com.google.protobuf.{CodedInputStream, WireFormat}
import io.circe.Json
import io.circe.syntax._
import restui.protobuf.data._
import restui.protobuf.json._

object Reader {
  implicit class ReaderOps(private val schema: Schema) {
    private val reader                                  = new Reader(schema)
    def read(buf: Array[Byte]): Either[Throwable, Json] = reader.read(buf)
    def read(buf: InputStream): Either[Throwable, Json] = reader.read(buf)
  }
}

class Reader(private val schema: Schema) {
  def read(buf: Array[Byte]): Either[Throwable, Json] =
    read(CodedInputStream.newInstance(buf), schema.root)

  def read(input: InputStream): Either[Throwable, Json] =
    read(CodedInputStream.newInstance(input), schema.root)

  private def read(input: CodedInputStream, messageSchema: Option[MessageSchema]): Either[Throwable, Json] =
    messageSchema match {
      case Some(messageSchema) =>
        for {
          result <- decodeInput(input, messageSchema).toList.traverse { case (key, json) => json.map(key -> _) }
          defaults = messageSchema.fields.valuesIterator.toList.collect {
            case Field(_, name, _, _, _, Some(default), _, _) if !result.exists(_._1 == name) => name -> default.asInstanceOf[Any].asJson
            case field @ Field(_, name, _, _, _, _, _, _) if !result.exists(_._1 == name)     => name -> setDefault(field)
          }
        } yield Json.obj(result ++ defaults: _*)
      case None => Json.Null.asRight
    }

  private def setDefault(field: Field): Json =
    if (field.label == Label.Repeated && !field.schema.flatMap(schema.messages.get(_)).exists(_.isMap)) Json.arr()
    else
      field.`type` match {
        case FieldDescriptor.Type.BOOL => Json.fromBoolean(false)
        case FieldDescriptor.Type.FIXED32 | FieldDescriptor.Type.SFIXED32 | FieldDescriptor.Type.SINT32 | FieldDescriptor.Type.INT32 |
            FieldDescriptor.Type.FIXED64 | FieldDescriptor.Type.SFIXED64 | FieldDescriptor.Type.SINT64 | FieldDescriptor.Type.INT64 =>
          Json.fromInt(0)
        case FieldDescriptor.Type.FLOAT | FieldDescriptor.Type.DOUBLE => Json.fromDoubleOrNull(0.0)
        case FieldDescriptor.Type.STRING | FieldDescriptor.Type.BYTES => Json.fromString("")
        case FieldDescriptor.Type.ENUM                                => Json.fromString(schema.enums(field.schema.get).values(1))
        case _                                                        => Json.Null
      }
  @tailrec
  private def decodeInput(input: CodedInputStream,
                          messageSchema: MessageSchema,
                          map: Map[String, Either[Throwable, Json]] = Map.empty): Map[String, Either[Throwable, Json]] =
    if (input.isAtEnd()) map
    else {
      val tag   = input.readTag()
      val id    = WireFormat.getTagFieldNumber(tag)
      val field = messageSchema.fields(id)
      val name  = field.name
      val value = field.label match {
        case Label.Repeated =>
          val maybeList = map.get(name).map(_.map(_.asArray.toVector.flatten)).getOrElse(Seq.empty[Json].asRight)
          val maybeResult =
            if (field.packed)
              input
                .readByteBuffer()
                .pipe(CodedInputStream.newInstance)
                .pipe(decodeList(_, field, maybeList.fold(_ => Nil, _.map(_.asRight[Throwable]))))
            else
              (for {
                list  <- maybeList
                value <- readValue(input, field)
              } yield list :+ value).fold(_ => Nil, _.map(_.asRight[Throwable]))
          maybeResult.toList.sequence.map { list =>
            field.schema.flatMap(schema.messages.get(_)) match {
              case Some(subSchema) if subSchema.isMap =>
                val obj = list.foldLeft(Seq.empty[(String, Json)]) {
                  case (acc, json) =>
                    val keyValues = json.asObject.toVector.flatMap(_.toVector).toMap
                    (for {
                      key   <- keyValues("key").asString
                      value <- keyValues.get("value")
                    } yield (key -> value)).fold(acc)(obj => acc :+ obj)
                }
                Json.obj(obj: _*)
              case _ => Json.arr(list: _*)
            }
          }
        case _ => readValue(input, field)
      }
      decodeInput(input, messageSchema, map + (name -> value))
    }

  @tailrec
  private def decodeList(input: CodedInputStream, field: Field, list: Seq[Either[Throwable, Json]]): Seq[Either[Throwable, Json]] =
    if (input.isAtEnd) list
    else
      decodeList(input, field, list :+ readValue(input, field))

  private def readValue(in: CodedInputStream, field: Field): Either[Throwable, Json] =
    field.`type` match {
      case Type.FLOAT    => Json.fromFloat(in.readFloat()).toRight(new IllegalArgumentException(""))
      case Type.DOUBLE   => Json.fromDouble(in.readDouble()).toRight(new IllegalArgumentException(""))
      case Type.FIXED32  => Json.fromInt(in.readFixed32()).asRight[Throwable]
      case Type.FIXED64  => Json.fromLong(in.readFixed64()).asRight[Throwable]
      case Type.INT32    => Json.fromInt(in.readInt32()).asRight[Throwable]
      case Type.INT64    => Json.fromLong(in.readInt64()).asRight[Throwable]
      case Type.UINT32   => Json.fromInt(in.readUInt32()).asRight[Throwable]
      case Type.UINT64   => Json.fromLong(in.readUInt64()).asRight[Throwable]
      case Type.SFIXED32 => Json.fromInt(in.readSFixed32()).asRight[Throwable]
      case Type.SFIXED64 => Json.fromLong(in.readSFixed64()).asRight[Throwable]
      case Type.SINT32   => Json.fromInt(in.readSInt32()).asRight[Throwable]
      case Type.SINT64   => Json.fromLong(in.readSInt64()).asRight[Throwable]
      case Type.BOOL     => Json.fromBoolean(in.readBool()).asRight[Throwable]
      case Type.STRING   => Json.fromString(in.readString()).asRight[Throwable]
      case Type.BYTES    => Json.fromString(new String(ju.Base64.getEncoder().encode(in.readByteArray()))).asRight[Throwable]
      case Type.ENUM     => Json.fromString(schema.enums(field.schema.get).values(in.readEnum())).asRight[Throwable]
      case Type.MESSAGE =>
        val nestedIn = CodedInputStream.newInstance(in.readByteBuffer())
        read(nestedIn, schema.messages(field.schema.get).some)
      case Type.GROUP => (new IllegalArgumentException("Unsupported type: GROUP")).asLeft
    }
}
