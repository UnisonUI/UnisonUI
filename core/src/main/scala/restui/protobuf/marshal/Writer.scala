package restui.protobuf.marshal
import java.io.{ByteArrayOutputStream, OutputStream}
import java.{util => ju}

import cats.implicits._
import com.google.protobuf.Descriptors.FieldDescriptor
import com.google.protobuf.Descriptors.FieldDescriptor.Type
import com.google.protobuf.{CodedOutputStream, WireFormat}
import io.circe.{DecodingFailure, Json}
import restui.protobuf.data._
import restui.protobuf.json._

object Writer {
  implicit class WriterOps(private val schema: Schema) {
    def write(json: Json): Either[Throwable, Array[Byte]] =
      new Writer(schema).write(json)
  }
}

class Writer(private val schema: Schema) {
  def write(json: Json): Either[Throwable, Array[Byte]] =
    schema.root.fold(Array.empty[Byte].asRight[Throwable]) { root =>
      val byteArrayOutputStream = new ByteArrayOutputStream()
      val result                = write(json.deepDropNullValues, root, byteArrayOutputStream)
      byteArrayOutputStream.close()
      val bytes = byteArrayOutputStream.toByteArray
      result.map(_ => bytes)
    }

  private def write(json: Json,
                    root: MessageSchema,
                    output: OutputStream): Either[Throwable, Unit] = {
    val cos    = CodedOutputStream.newInstance(output)
    val result = writeObject(json, root, cos)
    cos.flush()
    result
  }

  private def writeObject(
      json: Json,
      messageSchema: MessageSchema,
      output: CodedOutputStream): Either[Throwable, Unit] = {
    val fieldMap = messageSchema.fields.map { case (_, field) =>
      field.name -> field
    }
    val keyValues = json.asObject.toVector.flatMap(_.toVector)
    val fields = if (messageSchema.isMap) for {
      (key, value) <- keyValues
      keyField     <- fieldMap.get("key").toVector
      valueField   <- fieldMap.get("value").toVector
      tuple        <- (keyField, Json.fromString(key)) :: (valueField, value) :: Nil
    } yield tuple
    else
      for {
        (key, value) <- keyValues
        field        <- fieldMap.get(key).toVector
      } yield (field, value)
    fieldMap.find {
      case (_, Field(_, name, Label.Required, _, _, _, _, _)) =>
        !keyValues.exists(_._1 == name)
      case _ => false
    }.fold {
      fields.traverse { case (field, value) =>
        writeField(output, field, value)
      }.map(_ => ())
    } { case (_, Field(_, name, _, _, _, _, _, _)) =>
      Errors.RequiredField(name).asLeft
    }
  }

  private def writeField(output: CodedOutputStream,
                         field: Field,
                         value: Json): Either[Throwable, Unit] = {
    val wireTypeValue = wireType(field.`type`)
    field.label match {
      case Label.Repeated =>
        writeRepeat(output, field, value, wireTypeValue)
      case _ =>
        for {
          valueFromJson <- value.as[Any]
          result <-
            if (!field.default.contains(valueFromJson)) {
              output.writeTag(field.id, wireTypeValue)
              writeValue(output, field, value)
            } else ().asRight[Throwable]
        } yield result
    }
  }

  private def writeRepeat(output: CodedOutputStream,
                          field: Field,
                          value: Json,
                          wireTypeValue: Int): Either[Throwable, Unit] = {
    val maybeList = field.schema match {
      case Some(subSchema) if schema.messages.get(subSchema).exists(_.isMap) =>
        if (value.isObject)
          value.asObject.toVector.flatMap(_.toVector).map(Json.obj(_)).asRight
        else
          DecodingFailure(
            s""""${field.name}" is expecting: ${field.`type`.name}""",
            Nil).asLeft
      case _ => value.asArray.toVector.flatten.asRight
    }

    maybeList.flatMap { list =>
      if (field.packed) {
        val byteArrayOutputStream = new ByteArrayOutputStream()
        val bytesOut              = CodedOutputStream.newInstance(byteArrayOutputStream)
        val result                = list.traverse(writeValue(bytesOut, field, _)).map(_ => ())
        bytesOut.flush()
        output.writeByteArray(field.id, byteArrayOutputStream.toByteArray)
        result
      } else
        list.traverse { value =>
          output.writeTag(field.id, wireTypeValue)
          writeValue(output, field, value)
        }.map(_ => ())
    }
  }

  private def writeValue(out: CodedOutputStream,
                         field: Field,
                         value: Json): Either[Throwable, Unit] =
    (field.`type` match {
// $COVERAGE-OFF$
      case Type.FLOAT    => value.as[Float].map(out.writeFloatNoTag)
      case Type.DOUBLE   => value.as[Double].map(out.writeDoubleNoTag)
      case Type.FIXED32  => value.as[Int].map(out.writeFixed32NoTag)
      case Type.FIXED64  => value.as[Long].map(out.writeFixed64NoTag)
      case Type.INT32    => value.as[Int].map(out.writeInt32NoTag)
      case Type.INT64    => value.as[Long].map(out.writeInt64NoTag)
      case Type.UINT32   => value.as[Int].map(out.writeUInt32NoTag)
      case Type.UINT64   => value.as[Long].map(out.writeUInt64NoTag)
      case Type.SFIXED32 => value.as[Int].map(out.writeSFixed32NoTag)
      case Type.SFIXED64 => value.as[Long].map(out.writeSFixed64NoTag)
      case Type.SINT32   => value.as[Int].map(out.writeSInt32NoTag)
      case Type.SINT64   => value.as[Long].map(out.writeSInt64NoTag)
      case Type.BOOL     => value.as[Boolean].map(out.writeBoolNoTag)
// $COVERAGE-ON$
      case Type.STRING =>
        value.as[String].map(out.writeStringNoTag)
      case Type.BYTES =>
        value
          .as[String]
          .map(string =>
            out.writeByteArrayNoTag(ju.Base64.getDecoder.decode(string)))
      case Type.ENUM =>
        val enumMap = schema.enums(field.schema.get).values.map(_.swap)
        value.as[String].map(string => out.writeEnumNoTag(enumMap(string)))
      case Type.MESSAGE =>
        val baos     = new ByteArrayOutputStream()
        val bytesOut = CodedOutputStream.newInstance(baos)
        val result =
          writeObject(value, schema.messages(field.schema.get), bytesOut)
        bytesOut.flush()
        out.writeByteArrayNoTag(baos.toByteArray)
        result

      case Type.GROUP =>
        (new IllegalArgumentException("Unsupported type: GROUP")).asLeft[Unit]
    }).leftMap {
      case e: DecodingFailure =>
        e.withMessage(s""""${field.name}" is expecting: ${field.`type`.name}""")
      case e => e
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
