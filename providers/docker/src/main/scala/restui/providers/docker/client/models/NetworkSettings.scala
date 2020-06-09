package restui.providers.docker.client.models

import io.circe.syntax._
import io.circe.{Decoder, Encoder, HCursor, Json}
final case class NetworkSettings(networks: Map[String, Network])

object NetworkSettings {
  implicit val decoder: Decoder[NetworkSettings] = (cursor: HCursor) =>
    cursor
      .get[Map[String, Network]]("Networks")
      .map(NetworkSettings(_))

  implicit val encoder: Encoder[NetworkSettings] = (networkSettings: NetworkSettings) =>
    Json.obj("Networks" -> Json.obj(networkSettings.networks.view.mapValues(_.asJson).toList: _*))
}
