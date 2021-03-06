package tech.unisonui.grpc

import java.io.{ByteArrayInputStream, InputStream}

import io.circe.Json
import io.grpc.{KnownLength, MethodDescriptor}
import tech.unisonui.protobuf.data.Schema
import tech.unisonui.protobuf.marshal.Reader._
import tech.unisonui.protobuf.marshal.Writer._

class Marshaller(schema: Schema) extends MethodDescriptor.Marshaller[Json] {
  override def parse(stream: InputStream): Json =
    schema.read(stream).fold(e => throw e, identity(_))
  override def stream(value: Json): InputStream =
    schema
      .write(value)
      .fold(e => throw e,
            bytes => new ByteArrayInputStream(bytes) with KnownLength)
}
