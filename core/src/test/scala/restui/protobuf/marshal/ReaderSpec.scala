package restui.protobuf.marshal
import java.io.{ByteArrayInputStream, File, InputStream}
import java.nio.file.{Path, Paths}

import cats.syntax.either._
import cats.syntax.option._
import com.typesafe.scalalogging.LazyLogging
import io.circe.Json
import io.grpc.KnownLength
import org.scalatest.Inside
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import restui.protobuf.ProtobufCompiler
import restui.protobuf.data.Schema._
import restui.protobuf.marshal.Reader._

class ReaderSpec
    extends AnyWordSpec
    with Matchers
    with Inside
    with LazyLogging {
  private def arrayToStream(bytes: Array[Byte]): InputStream =
    new ByteArrayInputStream(bytes) with KnownLength
  implicit val compiler: ProtobufCompiler = new ProtobufCompiler {
    override def compile(path: Path): Either[Throwable, File] =
      new File(s"${path.toAbsolutePath.toString}set").asRight[Throwable]
    override def clean(file: File): Either[Throwable, Unit] = ().asRight
  }

  "marshaling a protobuf binary to json" should {
    "success" when {
      "there is no root key" in {
        val result = for {
          schema <- Paths.get("src/test/resources/helloworld.proto").toSchema
          input = arrayToStream(Array())
          json <- schema.read(input)
        } yield json

        inside(result) { case Right(json) =>
          json shouldBe Json.Null
        }
      }

      "all fields and types match the schema" in {
        val result = for {
          schema <- Paths.get("src/test/resources/helloworld.proto").toSchema
          input = arrayToStream(Array(10, 4, 116, 101, 115, 116))
          is    = schema.services("helloworld.Greeter").methods.head.inputType
          json <- is.read(input)
        } yield json

        inside(result) { case Right(json) =>
          json.noSpaces shouldBe """{"name":"test"}"""

        }
      }

      "with a complex type" in {
        val result = for {
          schema <- Paths.get("src/test/resources/complex.proto").toSchema
          input = arrayToStream(
            Array(18, 2, 0, 1, 26, 8, 10, 1, 107, 18, 3, 118, 97, 108, 26, 6,
              10, 1, 111, 18, 1, 97, 50, 5, 116, 101, 115, 116, 10, 10, 4, 116,
              101, 115, 116, 34, 10, 24, 1, 18, 2, 8, 1, 18, 2, 8, 2))
          json <- schema.copy(rootKey = "helloworld.Complex".some).read(input)
        } yield json

        inside(result) { case Right(json) =>
          json.noSpaces shouldBe """{"name":"test","myEnum":["VALUE1","VALUE2"],"myMap":{"k":"val","o":"a"},"myBytes":"dGVzdAo=","tree":{"root":true,"children":[{"value":1,"children":[],"root":false},{"value":2,"children":[],"root":false}],"value":0},"myInt":1}"""
        }
      }
    }
    /*
    "fail" when {
      "fields are missing from the schema" when {
        "the field is from a wrong type" in {
          val result = for {
            schema <- Paths.get("src/test/resources/complex.proto").toSchema
            input = parse("""{"name":"test","myMap":1}""").getOrElse(Json.Null)
            json <- schema.copy(rootKey = "helloworld.Complex".some).read(input)
          } yield json

          inside(result) {
            case Left(exception: DecodingFailure) => exception.getMessage shouldBe """"myMap" is expecting: MESSAGE"""
          }
        }

        "a required field is missing" in {
          val result = for {
            schema <- Paths.get("src/test/resources/complex.proto").toSchema
            input = parse("""{}""").getOrElse(Json.Null)
            json <- schema.copy(rootKey = "helloworld.Complex".some).read(input)
          } yield json

          inside(result) {
            case Left(exception: Errors.RequiredField) => exception.getMessage shouldBe "name is required"
          }
        }
      }
    }
     */
  }
}
