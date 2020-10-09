package restui.providers.webhook.models

import cats.syntax.functor._
import io.circe.Decoder
import io.circe.generic.semiauto._
import restui.models.Service.Grpc.Server

sealed trait Service {
  val name: String
  val metadata: Map[String, String]
}

object Service {
  implicit val decoderServer: Decoder[Server]   = deriveDecoder[Server]
  implicit val decoderOpenApi: Decoder[OpenApi] = deriveDecoder[OpenApi]
  implicit val decoderGrpc: Decoder[Grpc]       = deriveDecoder[Grpc]
  implicit val decoder: Decoder[Service] =
    List[Decoder[Service]](Decoder[OpenApi].widen, Decoder[Grpc].widen)
      .reduceLeft(_ or _)

  final case class OpenApi(name: String,
                           specification: String,
                           metadata: Map[String, String] = Map.empty)
      extends Service
  final case class Grpc(name: String,
                        schema: String,
                        servers: Map[String, Server],
                        metadata: Map[String, String] = Map.empty)
      extends Service
}
