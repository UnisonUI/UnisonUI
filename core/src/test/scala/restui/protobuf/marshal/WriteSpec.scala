package restui.protobuf.marshal
import java.io.File
import java.nio.file.{Path, Paths}

import cats.syntax.either._
import io.circe.Json
import io.circe.parser.parse
import org.scalatest.Inside
import org.scalatest.matchers.should.Matchers
import restui.protobuf.ProtobufCompiler
import restui.protobuf.data.Schema._
import restui.protobuf.marshal.Writer._
import org.scalatest.wordspec.AnyWordSpec
import com.typesafe.scalalogging.LazyLogging
import io.circe.DecodingFailure

class WriteSpec extends AnyWordSpec with Matchers with Inside with LazyLogging {
  implicit val compiler: ProtobufCompiler = new ProtobufCompiler {
    override def compile(path: Path): Either[Throwable, File] = new File(s"${path.toAbsolutePath().toString}set").asRight[Throwable]
    override def clean(file: File): Either[Throwable, Unit]   = ().asRight
  }

  "marshaling a json to protobuf binary" when {
    "all fields and types match the schema" should {
      "success" in {
        val result = for {
          schema <- Paths.get("src/test/resources/helloworld.proto").toSchema
          input = parse("""{"name":"test"}""").getOrElse(Json.Null)
          is    = schema.services("helloworld.Greeter").methods.head.inputType
          bytes <- is.write(input)
        } yield bytes

        inside(result) {
          case Right(bytes) => bytes shouldBe Array(10, 4, 116, 101, 115, 116)
        }
      }
    }

    "fields are missing from the schema" should {
      "fail" when {
        "the field is from a wrong type" in {
          val result = for {
            schema <- Paths.get("src/test/resources/helloworld.proto").toSchema
            input = parse("""{"name":1}""").getOrElse(Json.Null)
            is    = schema.services("helloworld.Greeter").methods.head.inputType
            bytes <- is.write(input)
          } yield bytes

          inside(result) {
            case Left(exception: DecodingFailure) => logger.info(exception.getMessage)
          }
        }
        "a required field is missing" in {
          val result = for {
            schema <- Paths.get("src/test/resources/helloworld.proto").toSchema
            input = parse("""{}""").getOrElse(Json.Null)
            is    = schema.services("helloworld.Greeter").methods.head.inputType
            bytes <- is.write(input)
          } yield bytes

          inside(result) {
            case Right(bytes) => bytes shouldBe Array(10, 4, 116, 101, 115, 116)
          }
        }
      }
    }
  }
}
