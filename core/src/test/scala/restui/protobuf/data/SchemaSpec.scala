package restui.protobuf.data

import java.io.File
import java.nio.file.{Path, Paths}

import cats.syntax.either._
import cats.syntax.option._
import com.google.protobuf.Descriptors.FieldDescriptor
import org.scalatest.Inside
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import restui.protobuf.ProtobufCompiler
import restui.protobuf.data.Schema._

class SchemaSpec extends AnyFlatSpec with Matchers with Inside {
  implicit val compiler: ProtobufCompiler = new ProtobufCompiler {
    override def compile(path: Path): Either[Throwable, File] =
      new File(s"${path.toAbsolutePath.toString}set").asRight[Throwable]
    override def clean(file: File): Either[Throwable, Unit] = ().asRight
  }
  private val expectedSchemaOneOf = Schema(
    Map(
      "helloworld.HelloRequest" -> MessageSchema(
        "helloworld.HelloRequest",
        Map(
          1 -> Field(1,
                     "name",
                     Label.Optional,
                     FieldDescriptor.Type.STRING,
                     packed = false,
                     None,
                     None,
                     None)),
        None,
        Map(
          "switch" -> Map(
            2 -> Field(2,
                       "myInt",
                       Label.Optional,
                       FieldDescriptor.Type.INT32,
                       packed = false,
                       None,
                       None,
                       None),
            3 -> Field(3,
                       "myString",
                       Label.Optional,
                       FieldDescriptor.Type.STRING,
                       packed = false,
                       None,
                       None,
                       None)
          ))
      ),
      "helloworld.HelloReply" -> MessageSchema(
        "helloworld.HelloReply",
        Map(
          1 -> Field(1,
                     "message",
                     Label.Optional,
                     FieldDescriptor.Type.STRING,
                     packed = false,
                     None,
                     None,
                     None)),
        None)
    ),
    Map(),
    Map(),
    None
  )
  private val expectedSchema = Schema(
    Map(
      "helloworld.HelloRequest" -> MessageSchema(
        "helloworld.HelloRequest",
        Map(
          1 -> Field(1,
                     "name",
                     Label.Optional,
                     FieldDescriptor.Type.STRING,
                     packed = false,
                     None,
                     None,
                     None)),
        None),
      "helloworld.HelloReply" -> MessageSchema(
        "helloworld.HelloReply",
        Map(
          1 -> Field(1,
                     "message",
                     Label.Optional,
                     FieldDescriptor.Type.STRING,
                     packed = false,
                     None,
                     None,
                     None)),
        None)
    ),
    Map(),
    Map(),
    None
  )

  it should "decode a valid protobuf schema without stream" in {
    inside(Paths.get("src/test/resources/helloworld.proto").toSchema) {
      case Right(schema) =>
        schema shouldBe expectedSchemaOneOf.copy(services = Map(
          "helloworld.Greeter" -> Service(
            "Greeter",
            "helloworld.Greeter",
            List(Method(
              "SayHello",
              expectedSchemaOneOf.copy(rootKey =
                "helloworld.HelloRequest".some),
              expectedSchemaOneOf.copy(rootKey = "helloworld.HelloReply".some),
              isServerStreaming = false,
              isClientStreaming = false
            ))
          )))
    }
  }

  it should "decode a valid protobuf schema with stream" in {
    inside(Paths.get("src/test/resources/helloworld_stream.proto").toSchema) {
      case Right(schema) =>
        schema shouldBe expectedSchema.copy(services = Map(
          "helloworld.Greeter" -> Service(
            "Greeter",
            "helloworld.Greeter",
            List(Method(
              "SayHello",
              expectedSchema.copy(rootKey = "helloworld.HelloRequest".some),
              expectedSchema.copy(rootKey = "helloworld.HelloReply".some),
              isServerStreaming = false,
              isClientStreaming = true
            ))
          )))

    }
  }
}
