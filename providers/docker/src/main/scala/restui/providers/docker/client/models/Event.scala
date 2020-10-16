package restui.providers.docker.client.models

import io.circe.{Decoder, Encoder, HCursor, Json}

final case class Event(id: String,
                       status: Option[State],
                       attributes: Map[String, String])

object Event {
  implicit val decoder: Decoder[Event] = (cursor: HCursor) =>
    for {
      id     <- cursor.get[String]("id")
      status <- cursor.get[String]("status").map(State.fromString)
      attributes <-
        cursor.downField("Actor").get[Map[String, String]]("Attributes")
    } yield Event(id, status, attributes)

  implicit val encoder: Encoder[Event] = (event: Event) =>
    Json.obj(
      "id" -> Json.fromString(event.id),
      "status" -> event.status.fold(Json.Null)(status =>
        Json.fromString(status.toString.toLowerCase)),
      "Actor" -> Json.obj(
        "Attributes" -> Json.obj(
          event.attributes.view.mapValues(Json.fromString).toList: _*)
      )
    )
}
