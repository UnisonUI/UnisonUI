package restui.specifications

import com.typesafe.scalalogging.LazyLogging
import io.circe.parser.{parse => parseJson}
import io.circe.schema.Schema
import restui.specifications.parse

import scala.io.Source
import scala.util.Try

object Validator extends LazyLogging {
  private val schema: Schema  = loadSchema("schema.json").get
  private val schema3: Schema = loadSchema("schema3.json").get

  private def loadSchema(file: String): Try[Schema] =
    for {
      input <- Try(Source.fromResource(file).getLines().mkString("\n"))
      json  <- parseJson(input).toTry
    } yield Schema.load(json)

  def isValid(input: String): Boolean =
    (for {
      json <- parse(input)
      result <-
        schema3
          .validate(json)
          .toEither
          .left
          .flatMap(_ => schema.validate(json).toEither)
    } yield result).isRight
}
