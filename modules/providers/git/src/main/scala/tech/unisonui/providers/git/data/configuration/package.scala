package tech.unisonui.providers.git.data

import cats.syntax.either._
import cats.syntax.functor._
import cats.syntax.option._
import io.circe.generic.auto._
import io.circe.{Decoder, DecodingFailure, HCursor}
package object configuration {
  sealed trait Versioned { val toConfiguration: Configuration }
  final case class Version1(name: Option[String],
                            specifications: List[OpenApi],
                            useProxy: Boolean)
      extends Versioned {
    override val toConfiguration: Configuration =
      Configuration(name, OpenApiSetting(specifications, useProxy).some, None)
  }

  final case class Version2(name: Option[String],
                            openapi: Option[OpenApiSetting],
                            grpc: Option[GrpcSetting])
      extends Versioned {
    override val toConfiguration: Configuration =
      Configuration(name, openapi, grpc)
  }

  object Versioned {
    implicit val decoder: Decoder[Versioned] =
      List[Decoder[Versioned]](Decoder[Version1].widen, Decoder[Version2].widen)
        .reduceLeft(_ or _)
  }

  object Version1 {
    implicit val decoder: Decoder[Version1] = (cursor: HCursor) =>
      for {
        name           <- cursor.get[Option[String]]("name")
        specifications <- cursor.get[List[OpenApi]]("specifications")
        useProxy       <- cursor.getOrElse[Boolean]("useProxy")(false)
      } yield Version1(name, specifications, useProxy)
  }

  object Version2 {
    implicit val decoder: Decoder[Version2] = (cursor: HCursor) =>
      for {
        version <- cursor.get[String]("version")
        _ <-
          if (version == "2") version.asRight[DecodingFailure]
          else DecodingFailure("incorrect version", Nil).asLeft[String]
        name    <- cursor.get[Option[String]]("name")
        openapi <- cursor.get[Option[OpenApiSetting]]("openapi")
        grpc    <- cursor.get[Option[GrpcSetting]]("grpc")
      } yield Version2(name, openapi, grpc)
  }
}
