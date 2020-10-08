package restui.protobuf.marshal
import java.io.File
import java.nio.file.{Path, Paths}

import cats.syntax.either._
import cats.syntax.option._
import com.typesafe.scalalogging.LazyLogging
import io.circe.parser.parse
import io.circe.{DecodingFailure, Json}
import org.scalatest.Inside
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import restui.protobuf.ProtobufCompiler
import restui.protobuf.data.Schema._
import restui.protobuf.marshal.Reader._
import restui.protobuf.marshal.Writer._

class WriteSpec extends AnyWordSpec with Matchers with Inside with LazyLogging {
  implicit val compiler: ProtobufCompiler = new ProtobufCompiler {
    override def compile(path: Path): Either[Throwable, File] = new File(s"${path.toAbsolutePath().toString}set").asRight[Throwable]
    override def clean(file: File): Either[Throwable, Unit]   = ().asRight
  }

  "marshaling a json to protobuf binary" should {
    "success" when {
      "all fields and types match the schema" in {
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

      "with a complex type" in {
        val result = for {
          schema <- Paths.get("src/test/resources/complex.proto").toSchema
          input = parse("""{"myEnum":["VALUE1","VALUE2"],"myMap":{"k":"val"},"name":"test"}""").getOrElse(Json.Null)
          bytes <- schema.copy(rootKey = "helloworld.Complex".some).write(input)
          r     <- schema.copy(rootKey = "helloworld.Complex".some).read(bytes)
          _ = logger.info("{}", r)
        } yield bytes

        inside(result) {
          case Right(bytes) => bytes shouldBe Array(18, 2, 0, 1, 26, 8, 10, 1, 107, 18, 3, 118, 97, 108, 10, 4, 116, 101, 115, 116)
        }
      }
    }

    "fail" when {
      "fields are missing from the schema" when {
        "the field is from a wrong type" in {
          val result = for {
            schema <- Paths.get("src/test/resources/complex.proto").toSchema
            input = parse("""{"myMap":1}""").getOrElse(Json.Null)
            bytes <- schema.copy(rootKey = "helloworld.Complex".some).write(input)
          } yield bytes

          inside(result) {
            case Left(exception: DecodingFailure) => exception.getMessage shouldBe """"myMap" is expecting: MESSAGE"""
          }
        }

        "a required field is missing" ignore {
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
