package restui.providers.git.git

import java.io.File

import cats.syntax.functor._
import io.circe.generic.auto._
import io.circe.syntax._
import io.circe.{Decoder, Encoder, HCursor, Json}

package object data {
  final case class Repository(uri: String,
                              branch: String,
                              specificationPaths: List[Specification],
                              directory: Option[File] = None,
                              serviceName: Option[String] = None)
  trait Specification
  final case class UnnamedSpecification(path: String)             extends Specification
  final case class NamedSpecification(name: String, path: String) extends Specification
  final case class RestUI(name: Option[String], specifications: List[Specification])

  object RestUI {
    implicit val encoder: Encoder[RestUI] = (restui: RestUI) => {
      val specifications = restui.specifications.asJson
      restui.name match {
        case None =>
          Json.obj(
            "specifications" -> specifications
          )
        case Some(name) =>
          Json.obj(
            "name"           -> Json.fromString(name),
            "specifications" -> specifications
          )
      }
    }
    implicit val decoder: Decoder[RestUI] = (cursor: HCursor) =>
      for {
        name           <- cursor.get[Option[String]]("name")
        specifications <- cursor.get[List[Specification]]("specifications")
      } yield RestUI(name, specifications)
  }

  object Specification {
    implicit val encoder: Encoder[Specification] = Encoder.instance {
      case specification: UnnamedSpecification => specification.asJson
      case specification: NamedSpecification   => specification.asJson
    }
    implicit val decoder: Decoder[Specification] =
      List[Decoder[Specification]](Decoder[UnnamedSpecification].widen, Decoder[NamedSpecification].widen).reduceLeft(_ or _)
  }

  object UnnamedSpecification {
    implicit val encoder: Encoder[UnnamedSpecification] = (specifications: UnnamedSpecification) => Json.fromString(specifications.path)
    implicit val decoder: Decoder[UnnamedSpecification] = (cursor: HCursor) => cursor.as[String].map(UnnamedSpecification(_))
  }

  object NamedSpecification {
    implicit val encoder: Encoder[NamedSpecification] = (specifications: NamedSpecification) =>
      Json.obj(
        "name" -> Json.fromString(specifications.name),
        "path" -> Json.fromString(specifications.path)
      )
    implicit val decoder: Decoder[NamedSpecification] = (cursor: HCursor) =>
      for {
        name <- cursor.get[String]("name")
        path <- cursor.get[String]("path")
      } yield NamedSpecification(name, path)
  }
}
