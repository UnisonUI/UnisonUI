package restui.providers.git.git

import java.io.File
import java.nio.file.Path

import cats.syntax.functor._
import io.circe.generic.auto._
import io.circe.{Decoder, HCursor}

package object data {
  final case class Repository(uri: String,
                              branch: String,
                              specificationPaths: List[Specification],
                              directory: Option[File] = None,
                              serviceName: Option[String] = None,
                              useProxy: Boolean = false)

  trait Specification extends Product with Serializable {
    val path: String
  }
  final case class UnnamedSpecification(path: String)                                        extends Specification
  final case class NamedSpecification(name: String, path: String, useProxy: Option[Boolean]) extends Specification
  final case class RestUI(name: Option[String], specifications: List[Specification], useProxy: Boolean)

  trait GitFileEvent extends Product with Serializable
  object GitFileEvent {
    final case class Deleted(path: Path)                                                        extends GitFileEvent
    final case class Upserted(maybeName: Option[String], path: Path, useProxy: Option[Boolean]) extends GitFileEvent
  }

  object RestUI {
    implicit val decoder: Decoder[RestUI] = (cursor: HCursor) =>
      for {
        name           <- cursor.get[Option[String]]("name")
        specifications <- cursor.get[List[Specification]]("specifications")
        useProxy       <- cursor.getOrElse[Boolean]("useProxy")(false)
      } yield RestUI(name, specifications, useProxy)
  }

  object Specification {
    implicit val decoder: Decoder[Specification] =
      List[Decoder[Specification]](Decoder[UnnamedSpecification].widen, Decoder[NamedSpecification].widen).reduceLeft(_ or _)
  }

  object UnnamedSpecification {
    implicit val decoder: Decoder[UnnamedSpecification] = (cursor: HCursor) => cursor.as[String].map(UnnamedSpecification(_))
  }

  object NamedSpecification {
    implicit val decoder: Decoder[NamedSpecification] = (cursor: HCursor) =>
      for {
        name     <- cursor.get[String]("name")
        path     <- cursor.get[String]("path")
        useProxy <- cursor.get[Option[Boolean]]("useProxy")
      } yield NamedSpecification(name, path, useProxy)
  }
}
