package restui.providers.git.git

import java.io.File
import java.nio.file.Path

import cats.syntax.functor._
import io.circe.generic.auto._
import io.circe.{Decoder, HCursor}
import restui.models.Service
import restui.protobuf.data.Schema

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

  final case class UnnamedSpecification(path: String) extends Specification
  final case class NamedSpecification(name: String,
                                      path: String,
                                      useProxy: Option[Boolean])
      extends Specification
  final case class RestUI(name: Option[String],
                          specifications: List[Specification],
                          grpc: Map[String, GrpcSetting],
                          useProxy: Option[Boolean])

  final case class GrpcSetting(maybeName: Option[String],
                               servers: Map[String, Service.Grpc.Server])

  sealed trait GitFileEvent extends Product with Serializable
  object GitFileEvent {
    final case class Deleted(path: Path) extends GitFileEvent
    final case class UpsertedOpenApi(maybeName: Option[String],
                                     path: Path,
                                     useProxy: Option[Boolean])
        extends GitFileEvent
    final case class UpsertedGrpc(maybeName: Option[String],
                                  path: Path,
                                  servers: Map[String, Service.Grpc.Server])
        extends GitFileEvent
  }

  sealed trait LoadedContent extends Product with Serializable
  object LoadedContent {
    final case class Deleted(path: Path) extends LoadedContent
    final case class OpenApi(maybeName: Option[String],
                             path: Path,
                             content: String,
                             useProxy: Option[Boolean])
        extends LoadedContent
    final case class Grpc(maybeName: Option[String],
                          path: Path,
                          schema: Schema,
                          servers: Map[String, Service.Grpc.Server])
        extends LoadedContent
  }

  object RestUI {
    implicit val decoder: Decoder[RestUI] = (cursor: HCursor) =>
      for {
        name           <- cursor.get[Option[String]]("name")
        specifications <- cursor.get[List[Specification]]("specifications")
        useProxy       <- cursor.get[Option[Boolean]]("useProxy")
        grpc           <- cursor.getOrElse[Map[String, GrpcSetting]]("grpc")(Map.empty)
      } yield RestUI(name, specifications, grpc, useProxy)
  }

  object GrpcSetting {
    implicit val decoder: Decoder[GrpcSetting] = (cursor: HCursor) =>
      for {
        maybeName <- cursor.get[Option[String]]("name")
        servers   <- cursor.get[List[(String, Service.Grpc.Server)]]("servers")
      } yield GrpcSetting(maybeName, servers.toMap)

    implicit val serverDecoder: Decoder[(String, Service.Grpc.Server)] =
      (cursor: HCursor) =>
        for {
          address <- cursor.get[String]("address")
          port    <- cursor.get[Int]("port")
          useTls  <- cursor.getOrElse[Boolean]("useTls")(false)
          name    <- cursor.getOrElse[String]("name")(s"$address:$port")
        } yield name -> Service.Grpc.Server(address, port, useTls)
  }

  object Specification {
    implicit val decoder: Decoder[Specification] =
      List[Decoder[Specification]](
        Decoder[UnnamedSpecification].widen,
        Decoder[NamedSpecification].widen).reduceLeft(_ or _)
  }

  object UnnamedSpecification {
    implicit val decoder: Decoder[UnnamedSpecification] = (cursor: HCursor) =>
      cursor.as[String].map(UnnamedSpecification(_))
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
