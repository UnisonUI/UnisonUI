package restui.protobuf.marshal
import java.io.{ByteArrayOutputStream, OutputStream}
import java.{util => ju}

import com.google.protobuf.Descriptors.FieldDescriptor
import com.google.protobuf.Descriptors.FieldDescriptor.Type
import com.google.protobuf.{CodedOutputStream, WireFormat}
import io.circe.Json
import restui.protobuf.data._
import restui.protobuf.json._

object Writer {
  implicit class WriterOps(private val schema: Schema) {
    def write(json: Json): Array[Byte] = new Writer(schema).write(json)
  }
}

class Writer(private val schema: Schema) {
  def write(json: Json): Array[Byte] = {
    val byteArrayOutputStream = new ByteArrayOutputStream()
    schema.root.foreach { root =>
      write(json, root, byteArrayOutputStream)
    }
    byteArrayOutputStream.close
    byteArrayOutputStream.toByteArray()
  }

  private def write(json: Json, root: MessageSchema, output: OutputStream): Unit = {
    val cos = CodedOutputStream.newInstance(output)
    writeObject(json, root, cos)
    cos.flush()
  }

  private def writeObject(json: Json, messageSchema: MessageSchema, output: CodedOutputStream): Unit = {
    val fieldMap = messageSchema.fields.map { case (_, field) => field.name -> field }
    for {
      jsonObject   <- json.asObject.toVector
      (key, value) <- jsonObject.toVector
      field        <- fieldMap.get(key).toVector
      _ = writeRepeat(output, field, value)
    } yield ()
  }

  private def writeRepeat(output: CodedOutputStream, field: Field, value: Json): Unit = {
    val wireTypeValue = wireType(field.`type`)
    field.label match {
      case Label.Repeated =>
        if (field.packed) {}
      case _ =>
        if (!field.default.contains(value.as[Any])) {
          output.writeTag(field.id, wireTypeValue)
          writeValue(output, field, value)
        }
    }
  }

  private def writeValue(out: CodedOutputStream, field: Field, value: Json): Unit =
    field.`type` match {
      case Type.FLOAT    => value.asNumber.foreach(number => out.writeFloatNoTag(number.toFloat))
      case Type.DOUBLE   => value.asNumber.foreach(number => out.writeDoubleNoTag(number.toDouble))
      case Type.FIXED32  => value.asNumber.flatMap(_.toInt).foreach(out.writeFixed32NoTag(_))
      case Type.FIXED64  => value.asNumber.flatMap(_.toLong).foreach(out.writeFixed64NoTag(_))
      case Type.INT32    => value.asNumber.flatMap(_.toInt).foreach(out.writeInt32NoTag(_))
      case Type.INT64    => value.asNumber.flatMap(_.toLong).foreach(out.writeInt64NoTag(_))
      case Type.UINT32   => value.asNumber.flatMap(_.toInt).foreach(out.writeUInt32NoTag(_))
      case Type.UINT64   => value.asNumber.flatMap(_.toLong).foreach(out.writeUInt64NoTag(_))
      case Type.SFIXED32 => value.asNumber.flatMap(_.toInt).foreach(out.writeSFixed32NoTag(_))
      case Type.SFIXED64 => value.asNumber.flatMap(_.toLong).foreach(out.writeSFixed64NoTag(_))
      case Type.SINT32   => value.asNumber.flatMap(_.toInt).foreach(out.writeSInt32NoTag(_))
      case Type.SINT64   => value.asNumber.flatMap(_.toLong).foreach(out.writeSInt64NoTag(_))
      case Type.BOOL     => value.asBoolean.foreach(out.writeBoolNoTag(_))
      case Type.STRING   => value.asString.foreach(out.writeStringNoTag(_))
      case Type.BYTES    => value.asString.foreach(string => out.writeByteArrayNoTag(ju.Base64.getDecoder.decode(string)))
      case Type.ENUM =>
        val enumMap = schema.enums(field.schema.get).values.map(_.swap)
        value.asString.foreach(string => out.writeEnumNoTag(enumMap(string)))
      case Type.MESSAGE =>
        val baos     = new ByteArrayOutputStream()
        val bytesOut = CodedOutputStream.newInstance(baos)
        writeObject(value, schema.messages(field.schema.get), bytesOut)
        bytesOut.flush()
        out.writeByteArrayNoTag(baos.toByteArray)
      case Type.GROUP => throw new IllegalArgumentException("Unsupported type: GROUP")
    }

  private def wireType(fieldType: FieldDescriptor.Type): Int =
    (fieldType match {
      case Type.FLOAT    => WireFormat.FieldType.FLOAT
      case Type.DOUBLE   => WireFormat.FieldType.DOUBLE
      case Type.FIXED32  => WireFormat.FieldType.FIXED32
      case Type.FIXED64  => WireFormat.FieldType.FIXED64
      case Type.INT32    => WireFormat.FieldType.INT32
      case Type.INT64    => WireFormat.FieldType.INT64
      case Type.UINT32   => WireFormat.FieldType.UINT32
      case Type.UINT64   => WireFormat.FieldType.UINT64
      case Type.SFIXED32 => WireFormat.FieldType.SFIXED32
      case Type.SFIXED64 => WireFormat.FieldType.SFIXED64
      case Type.SINT32   => WireFormat.FieldType.SINT32
      case Type.SINT64   => WireFormat.FieldType.SINT64
      case Type.BOOL     => WireFormat.FieldType.BOOL
      case Type.STRING   => WireFormat.FieldType.STRING
      case Type.BYTES    => WireFormat.FieldType.BYTES
      case Type.ENUM     => WireFormat.FieldType.ENUM
      case Type.MESSAGE  => WireFormat.FieldType.MESSAGE
      case Type.GROUP    => WireFormat.FieldType.GROUP
    }).getWireType
}
