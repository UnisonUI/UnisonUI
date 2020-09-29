package restui.providers.git.git.data

import cats.syntax.either._
import io.circe.yaml.parser
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

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
    restui shouldBe RestUI(Some("test"),
                           UnnamedSpecification("file.yaml") :: NamedSpecification("another service", "other.yaml", None) :: Nil,
                           None)
  }
}
