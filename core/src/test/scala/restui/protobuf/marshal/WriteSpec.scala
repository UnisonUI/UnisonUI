package restui.protobuf.marshal
import java.io.File
import java.nio.file.{Path, Paths}

import cats.syntax.either._
import cats.syntax.option._
import io.circe.parser.parse
import io.circe.{DecodingFailure, Json}
import org.scalatest.Inside
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import restui.protobuf.ProtobufCompiler
import restui.protobuf.data.Schema._
import restui.protobuf.marshal.Writer._

class WriteSpec extends AnyWordSpec with Matchers with Inside {
  implicit val compiler: ProtobufCompiler = new ProtobufCompiler {
    override def compile(path: Path): Either[Throwable, File] =
      new File(s"${path.toAbsolutePath.toString}set").asRight[Throwable]
    override def clean(file: File): Either[Throwable, Unit] = ().asRight
  }

  "marshaling a json to protobuf binary" should {
    "success" when {
      "there is no root key" in {
        val result = for {
          schema <- Paths.get("src/test/resources/helloworld.proto").toSchema
          input = parse("""{"name":"test"}""").getOrElse(Json.Null)
          bytes <- schema.write(input)
        } yield bytes

        inside(result) { case Right(bytes) =>
          bytes shouldBe empty
        }
      }

      "there is an oneof field" in {
        val result = for {
          schema <- Paths.get("src/test/resources/helloworld.proto").toSchema
          input = parse(
            """{"name":"test","switch":{"type":"myString","value":"test"}}""")
            .getOrElse(Json.Null)
          bytes <- schema
            .services("helloworld.Greeter")
            .methods
            .head
            .inputType
            .write(input)
        } yield bytes

        inside(result) { case Right(bytes) =>
          bytes shouldBe Array(10, 4, 116, 101, 115, 116, 26, 4, 116, 101, 115,
            116)
        }
      }

      "all fields and types match the schema" in {
        val result = for {
          schema <- Paths.get("src/test/resources/helloworld.proto").toSchema
          input = parse("""{"name":"test"}""").getOrElse(Json.Null)
          is    = schema.services("helloworld.Greeter").methods.head.inputType
          bytes <- is.write(input)
        } yield bytes

        inside(result) { case Right(bytes) =>
          bytes shouldBe Array(10, 4, 116, 101, 115, 116)
        }
      }

      "with a complex type" in {
        val result = for {
          schema <- Paths.get("src/test/resources/complex.proto").toSchema
          input = parse("""{
          |  "myEnum":["VALUE1","VALUE2"],
          |  "myMap":{"k":"val","o":"a"},
          |  "myBytes":"dGVzdAo=",
          |  "name":"test",
          |  "tree":{
          |    "value": 0,
          |    "root": true,
          |    "children": [
          |      {"value":1,"children":[]},
          |      {"value":2,"children":[]}
          |    ]
          | }
          |}""".stripMargin).getOrElse(Json.Null)
          bytes <- schema.copy(rootKey = "helloworld.Complex".some).write(input)
        } yield bytes

        inside(result) { case Right(bytes) =>
          bytes shouldBe Array(18, 2, 0, 1, 26, 8, 10, 1, 107, 18, 3, 118, 97,
            108, 26, 6, 10, 1, 111, 18, 1, 97, 50, 5, 116, 101, 115, 116, 10,
            10, 4, 116, 101, 115, 116, 34, 10, 24, 1, 18, 2, 8, 1, 18, 2, 8, 2)
        }
      }
    }

    "fail" when {
      "fields are missing from the schema" when {
        "the field is from a wrong type" in {
          val result = for {
            schema <- Paths.get("src/test/resources/complex.proto").toSchema
            input = parse("""{"name":"test","myMap":1}""").getOrElse(Json.Null)
            bytes <-
              schema.copy(rootKey = "helloworld.Complex".some).write(input)
          } yield bytes

          inside(result) { case Left(exception: DecodingFailure) =>
            exception.getMessage shouldBe """"myMap" is expecting: MESSAGE"""
          }
        }

        "a required field is missing" in {
          val result = for {
            schema <- Paths.get("src/test/resources/complex.proto").toSchema
            input = parse("""{}""").getOrElse(Json.Null)
            bytes <-
              schema.copy(rootKey = "helloworld.Complex".some).write(input)
          } yield bytes

          inside(result) { case Left(exception: Errors.RequiredField) =>
            exception.getMessage shouldBe "name is required"
          }
        }
      }
    }
  }
}
