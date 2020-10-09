package restui.providers.git.git.data

import cats.syntax.either._
import cats.syntax.option._
import io.circe.yaml.parser
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import restui.models.Service

class RestUISpec extends AnyFlatSpec with Matchers {
  it should "decode a restui config file" in {
    val input = """name: "test"
|specifications:
| - "file.yaml"
| - name: "another service"
|   path: "other.yaml"
""".stripMargin
    val restui = parser
      .parse(input)
      .flatMap(_.as[RestUI])
      .valueOr(throw _)
    restui shouldBe RestUI(
      "test".some,
      UnnamedSpecification("file.yaml") :: NamedSpecification("another service",
                                                              "other.yaml",
                                                              None) :: Nil,
      Map.empty,
      None)
  }
  it should "decode a restui config file with grpc" in {
    val input = """name: "test"
|specifications:
| - "file.yaml"
| - name: "another service"
|   path: "other.yaml"
|grpc:
|  "path/spec.proto":
|    servers:
|      - address: 127.0.0.1
|        port: 8080
|  "path/spec2.proto":
|    name: test
|    servers:
|      - address: 127.0.0.1
|        port: 8080
|        name: other server
|        useTls: true
""".stripMargin
    val restui = parser
      .parse(input)
      .flatMap(_.as[RestUI])
      .valueOr(throw _)
    restui shouldBe RestUI(
      "test".some,
      UnnamedSpecification("file.yaml") :: NamedSpecification("another service",
                                                              "other.yaml",
                                                              None) :: Nil,
      Map(
        "path/spec.proto" -> GrpcSetting(
          None,
          Map(
            "127.0.0.1:8080" -> Service.Grpc.Server("127.0.0.1", 8080, false))),
        "path/spec2.proto" -> GrpcSetting(
          "test".some,
          Map("other server" -> Service.Grpc.Server("127.0.0.1", 8080, true)))
      ),
      None
    )
  }

}
