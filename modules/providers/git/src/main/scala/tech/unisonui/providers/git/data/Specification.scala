package tech.unisonui.providers.git.data
import cats.syntax.functor._
import cats.syntax.option._
import io.circe.{Decoder, HCursor}
import tech.unisonui.models.Service

sealed trait Specification extends Product with Serializable {
  val serviceName: Option[String]
  val path: String
}

sealed trait OpenApi extends Product with Serializable with Specification {
  val useProxy: Option[Boolean]
}

final case class UnnamedOpenApi(path: String, useProxy: Option[Boolean] = None)
    extends OpenApi {
  val serviceName: Option[String] = None
}

final case class NamedOpenApi(name: String,
                              path: String,
                              useProxy: Option[Boolean])
    extends OpenApi { val serviceName: Option[String] = name.some }

final case class Grpc(serviceName: Option[String],
                      path: String,
                      servers: Map[String, Service.Grpc.Server])
    extends Specification

object OpenApi {
  implicit val decoder: Decoder[OpenApi] =
    List[Decoder[OpenApi]](Decoder[UnnamedOpenApi].widen,
                           Decoder[NamedOpenApi].widen).reduceLeft(_ or _)
}

object UnnamedOpenApi {
  implicit val decoder: Decoder[UnnamedOpenApi] = (cursor: HCursor) =>
    cursor.as[String].map(UnnamedOpenApi(_))
}

object NamedOpenApi {
  implicit val decoder: Decoder[NamedOpenApi] = (cursor: HCursor) =>
    for {
      name     <- cursor.get[String]("name")
      path     <- cursor.get[String]("path")
      useProxy <- cursor.get[Option[Boolean]]("useProxy")
    } yield NamedOpenApi(name, path, useProxy)
}
