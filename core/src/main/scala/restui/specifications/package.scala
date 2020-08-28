package restui
import io.circe.parser.{parse => parseJson}
import io.circe.yaml.parser.{parse => parseYaml}
import io.circe.{Error, Json}

package object specifications {
  def parse(input: String): Either[Error, Json] = parseYaml(input).left.flatMap(_ => parseJson(input))
}
