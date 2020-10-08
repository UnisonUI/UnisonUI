package restui.grpc

import java.io.{ByteArrayInputStream, InputStream}

import io.circe.Json
import io.grpc.{KnownLength, MethodDescriptor}
import restui.protobuf.data.Schema
import restui.protobuf.marshal.Reader._
import restui.protobuf.marshal.Writer._

class Marshaller(schema: Schema) extends MethodDescriptor.Marshaller[Json] {
  override def parse(stream: InputStream): Json = schema.read(stream)
  override def stream(value: Json): InputStream =
    schema.write(value).fold(e => throw e, bytes => new ByteArrayInputStream(bytes) with KnownLength)

}
