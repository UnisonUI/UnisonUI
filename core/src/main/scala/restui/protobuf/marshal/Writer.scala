package restui.protobuf.marshal
import java.io.{ByteArrayOutputStream, OutputStream}
import java.{util => ju}

import cats.implicits._
import com.google.protobuf.Descriptors.FieldDescriptor
import com.google.protobuf.Descriptors.FieldDescriptor.Type
import com.google.protobuf.{CodedOutputStream, WireFormat}
import io.circe.Json
import restui.protobuf.data._
import restui.protobuf.json._
import io.circe.DecodingFailure

object Writer {
  implicit class WriterOps(private val schema: Schema) {
    def write(json: Json): Either[Throwable, Array[Byte]] = new Writer(schema).write(json)
  }
}

class Writer(private val schema: Schema) {
  def write(json: Json): Either[Throwable, Array[Byte]] =
    schema.root.fold(Array.empty[Byte].asRight[Throwable]) { root =>
      val byteArrayOutputStream = new ByteArrayOutputStream()
      val result                = write(json, root, byteArrayOutputStream)
      byteArrayOutputStream.close
      val bytes = byteArrayOutputStream.toByteArray()
      result.map(_ => bytes)
    }

  private def write(json: Json, root: MessageSchema, output: OutputStream): Either[Throwable, Unit] = {
    val cos    = CodedOutputStream.newInstance(output)
    val result = writeObject(json, root, cos)
    cos.flush()
    result
  }

  private def writeObject(json: Json, messageSchema: MessageSchema, output: CodedOutputStream): Either[Throwable, Unit] = {
    val fieldMap = messageSchema.fields.map { case (_, field) => field.name -> field }
    val fields = for {
      jsonObject   <- json.asObject.toVector
      (key, value) <- jsonObject.toVector
      field        <- fieldMap.get(key).toVector
    } yield (field, value)
    fields.traverse { case (field, value) => writeField(output, field, value) }.map(_ => ())
  }

  private def writeField(output: CodedOutputStream, field: Field, value: Json): Either[Throwable, Unit] = {
    val wireTypeValue = wireType(field.`type`)
    field.label match {
      case Label.Repeated =>
        writeRepeat(output, field, value, wireTypeValue)
      case _ =>
        if (!field.default.contains(value.as[Any])) {
          output.writeTag(field.id, wireTypeValue)
          writeValue(output, field, value)
        } else ().asRight
    }
  }

  private def writeRepeat(output: CodedOutputStream, field: Field, value: Json, wireTypeValue: Int): Either[Throwable, Unit] = {
    val list = value.asArray.toVector.flatten
    if (field.packed) {
      val byteArrayOutputStream = new ByteArrayOutputStream()
      val bytesOut              = CodedOutputStream.newInstance(byteArrayOutputStream)
      val result                = list.traverse(writeValue(bytesOut, field, _)).map(_ => ())
      bytesOut.flush()
      output.writeByteArray(field.id, byteArrayOutputStream.toByteArray())
      result
    } else
      list.traverse { value =>
        output.writeTag(field.id, wireTypeValue)
        writeValue(output, field, value)
      }.map(_ => ())
  }

  private def writeValue(out: CodedOutputStream, field: Field, value: Json): Either[Throwable, Unit] =
    (field.`type` match {
// $COVERAGE-OFF$
      case Type.FLOAT    => value.as[Float].map(out.writeFloatNoTag(_))
      case Type.DOUBLE   => value.as[Double].map(out.writeDoubleNoTag(_))
      case Type.FIXED32  => value.as[Int].map(out.writeFixed32NoTag(_))
      case Type.FIXED64  => value.as[Long].map(out.writeFixed64NoTag(_))
      case Type.INT32    => value.as[Int].map(out.writeInt32NoTag(_))
      case Type.INT64    => value.as[Long].map(out.writeInt64NoTag(_))
      case Type.UINT32   => value.as[Int].map(out.writeUInt32NoTag(_))
      case Type.UINT64   => value.as[Long].map(out.writeUInt64NoTag(_))
      case Type.SFIXED32 => value.as[Int].map(out.writeSFixed32NoTag(_))
      case Type.SFIXED64 => value.as[Long].map(out.writeSFixed64NoTag(_))
      case Type.SINT32   => value.as[Int].map(out.writeSInt32NoTag(_))
      case Type.SINT64   => value.as[Long].map(out.writeSInt64NoTag(_))
      case Type.BOOL     => value.as[Boolean].map(out.writeBoolNoTag(_))
// $COVERAGE-ON$
      case Type.STRING =>
        value.as[String].map(out.writeStringNoTag(_))
      case Type.BYTES => value.as[String].map(string => out.writeByteArrayNoTag(ju.Base64.getDecoder.decode(string)))
      case Type.ENUM =>
        val enumMap = schema.enums(field.schema.get).values.map(_.swap)
        value.as[String].map(string => out.writeEnumNoTag(enumMap(string)))
      case Type.MESSAGE =>
        val baos     = new ByteArrayOutputStream()
        val bytesOut = CodedOutputStream.newInstance(baos)
        val result   = writeObject(value, schema.messages(field.schema.get), bytesOut)
        bytesOut.flush()
        out.writeByteArrayNoTag(baos.toByteArray)
        result
      case Type.GROUP => (new IllegalArgumentException("Unsupported type: GROUP")).asLeft[Unit]
    }).leftMap {
      case e: DecodingFailure => e.withMessage(s""""${field.name}" is expecting: ${field.`type`.name}""")
      case e                  => e
    }

// $COVERAGE-OFF$
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
// $COVERAGE-ON$
}
