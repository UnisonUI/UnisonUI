package restui.providers.docker.client.models

import io.circe.{Decoder, Encoder, HCursor, Json}

final case class Network(ipAddress: String)

object Network {
  implicit val decoder: Decoder[Network] = (cursor: HCursor) =>
    cursor.get[String]("IPAddress").map(Network(_))

  implicit val encoder: Encoder[Network] = (network: Network) =>
    Json.obj("IPAddress" -> Json.fromString(network.ipAddress))
}
