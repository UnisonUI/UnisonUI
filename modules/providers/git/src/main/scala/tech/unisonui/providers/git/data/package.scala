package tech.unisonui.providers.git

import java.io.File
import java.nio.file.Path

import tech.unisonui.models.Service
import tech.unisonui.protobuf.data.Schema
package object data {
  final case class Repository(uri: String,
                              branch: String,
                              specifications: List[Specification] = Nil,
                              serviceName: Option[String] = None,
                              directory: Option[File] = None)

  sealed trait GitFileEvent extends Product with Serializable

  object GitFileEvent {
    final case class Deleted(path: Path) extends GitFileEvent
    final case class UpsertedOpenApi(maybeName: Option[String],
                                     path: Path,
                                     useProxy: Boolean)
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
                             useProxy: Boolean)
        extends LoadedContent

    final case class Grpc(maybeName: Option[String],
                          path: Path,
                          schema: Schema,
                          servers: Map[String, Service.Grpc.Server])
        extends LoadedContent
  }
}
