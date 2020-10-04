package restui.protobuf.marshal
import java.io.InputStream
import java.nio.ByteBuffer
import java.{util => ju}

import scala.collection.mutable

import cats.syntax.option._
import com.google.protobuf.Descriptors.FieldDescriptor.Type
import com.google.protobuf.{CodedInputStream, WireFormat}
import io.circe.Json
import io.circe.syntax._
import restui.protobuf.data._
import restui.protobuf.json._

object Reader {
  implicit class ReaderOps(private val schema: Schema) {
    private val reader               = new Reader(schema)
    def read(buf: Array[Byte]): Json = reader.read(buf)
    def read(buf: ByteBuffer): Json  = reader.read(buf)
    def read(buf: InputStream): Json = reader.read(buf)
  }
}

class Reader(private val schema: Schema) {
  def read(buf: Array[Byte]): Json =
    read(CodedInputStream.newInstance(buf), schema.root)

  def read(buf: ByteBuffer): Json =
    read(CodedInputStream.newInstance(buf), schema.root)

  def read(input: InputStream): Json =
    read(CodedInputStream.newInstance(input), schema.root)

  private def read(input: CodedInputStream, messageSchema: Option[MessageSchema]): Json =
    messageSchema match {
      case Some(messageSchema) =>
        val map = mutable.Map.empty[Int, Json]
        while (!input.isAtEnd) {
          val tag   = input.readTag()
          val id    = WireFormat.getTagFieldNumber(tag)
          val field = messageSchema.fields(id)
          field.label match {
            case Label.Repeated =>
              val list = mutable.ListBuffer.from(map.get(id).map(_.asArray.toVector.flatten).getOrElse(Nil))
              if (field.packed) {
                val bytesIn = CodedInputStream.newInstance(input.readByteBuffer())
                while (!bytesIn.isAtEnd)
                  list += readValue(bytesIn, field)
              } else list += readValue(input, field)
              map.put(id, Json.arr(list.toList: _*))
            case _ => map.put(id, readValue(input, field))
          }
        }
        val result = map.toList.map { case (index, value) => messageSchema.fields(index).name -> value }
        val defaults = messageSchema.fields.valuesIterator.toList.collect {
          case Field(_, name, _, _, _, Some(default), _, _) if !result.contains(name) => name -> default.asInstanceOf[Any].asJson
        }
        Json.obj(result ++ defaults: _*)

      case None => Json.Null
    }

  private def readValue(in: CodedInputStream, field: Field): Json =
    field.`type` match {
      case Type.FLOAT    => Json.fromFloat(in.readFloat()).get
      case Type.DOUBLE   => Json.fromDouble(in.readDouble()).get
      case Type.FIXED32  => Json.fromInt(in.readFixed32())
      case Type.FIXED64  => Json.fromLong(in.readFixed64())
      case Type.INT32    => Json.fromInt(in.readInt32())
      case Type.INT64    => Json.fromLong(in.readInt64())
      case Type.UINT32   => Json.fromInt(in.readUInt32())
      case Type.UINT64   => Json.fromLong(in.readUInt64())
      case Type.SFIXED32 => Json.fromInt(in.readSFixed32())
      case Type.SFIXED64 => Json.fromLong(in.readSFixed64())
      case Type.SINT32   => Json.fromInt(in.readSInt32())
      case Type.SINT64   => Json.fromLong(in.readSInt64())
      case Type.BOOL     => Json.fromBoolean(in.readBool())
      case Type.STRING   => Json.fromString(in.readString())
      case Type.BYTES    => Json.fromString(new String(ju.Base64.getEncoder().encode(in.readByteArray())))
      case Type.ENUM     => Json.fromString(schema.enums(field.schema.get).values(in.readEnum()))
      case Type.MESSAGE =>
        val nestedIn = CodedInputStream.newInstance(in.readByteBuffer())
        read(nestedIn, schema.messages(field.schema.get).some)
      case Type.GROUP => throw new IllegalArgumentException("Unsupported type: GROUP")
    }
}
