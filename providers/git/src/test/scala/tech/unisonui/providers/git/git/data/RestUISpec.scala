package tech.unisonui.providers.git.git.data

import cats.syntax.either._
import cats.syntax.option._
import io.circe.yaml.parser
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import tech.unisonui.models.Service

class RestUISpec extends AnyFlatSpec with Matchers {
  it should "decode a unisonui config file" in {
    val input = """name: "test"
|specifications:
| - "file.yaml"
| - name: "another service"
|   path: "other.yaml"
""".stripMargin
    val unisonui = parser
      .parse(input)
      .flatMap(_.as[RestUI])
      .valueOr(throw _)
    unisonui shouldBe RestUI(
      "test".some,
      UnnamedSpecification("file.yaml") :: NamedSpecification("another service",
                                                              "other.yaml",
                                                              None) :: Nil,
      None,
      None)
  }
  it should "decode a unisonui config file with grpc" in {
    val input = """name: "test"
|specifications:
| - "file.yaml"
| - name: "another service"
|   path: "other.yaml"
|grpc:
|  protobufs:
|    "path/spec.proto":
|      servers:
|        - address: 127.0.0.1
|          port: 8080
|    "path/spec2.proto":
|      name: test
|      servers:
|        - address: 127.0.0.1
|          port: 8080
|          name: other server
|          useTls: true
""".stripMargin
    val unisonui = parser
      .parse(input)
      .flatMap(_.as[RestUI])
      .valueOr(throw _)
    unisonui shouldBe RestUI(
      "test".some,
      UnnamedSpecification("file.yaml") :: NamedSpecification("another service",
                                                              "other.yaml",
                                                              None) :: Nil,
      GrpcSetting(
        Map.empty,
        Map(
          "path/spec.proto" -> ProtobufSetting(
            None,
            Map("127.0.0.1:8080" -> Service.Grpc
              .Server("127.0.0.1", 8080, useTls = false))),
          "path/spec2.proto" -> ProtobufSetting(
            "test".some,
            Map("other server" -> Service.Grpc
              .Server("127.0.0.1", 8080, useTls = true)))
        )
      ).some,
      None
    )
  }

}
