package restui.protobuf.grpc

import java.io.{ByteArrayInputStream, InputStream}

import io.circe.Json
import io.grpc.KnownLength
import restui.protobuf.Reader._
import restui.protobuf.Writer._
import restui.protobuf.data.Schema

class Marshaller(schema: Schema) extends io.grpc.MethodDescriptor.Marshaller[Json] {
  override def parse(stream: InputStream): Json = schema.read(stream)
  override def stream(value: Json): InputStream = new ByteArrayInputStream(schema.write(value)) with KnownLength

}
