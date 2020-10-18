package tech.unisonui.providers.docker.client.models

import io.circe.syntax._
import io.circe.{Decoder, Encoder, HCursor, Json}

final case class Container(labels: Map[String, String], ip: Option[String])

object Container {
  implicit val decoder: Decoder[Container] = (cursor: HCursor) =>
    for {
      labels          <- cursor.downField("Config").get[Map[String, String]]("Labels")
      networkSettings <- cursor.get[NetworkSettings]("NetworkSettings")
      ip = networkSettings.networks.headOption.map(_._2.ipAddress)
    } yield Container(labels, ip)

  implicit val encoder: Encoder[Container] = (container: Container) => {
    val obj =
      container.ip.foldLeft(
        "Config" ->
          Json.obj(
            "Labels" ->
              Json.obj(container.labels.view
                .mapValues(Json.fromString)
                .toList: _*)) :: Nil)((obj, ip) =>
        obj :+ "NetworkSettings" -> NetworkSettings(
          Map("Bridge" -> Network(ip))).asJson)
    Json.obj(obj: _*)
  }

}
