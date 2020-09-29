package restui.models
import io.circe.{Encoder, Json}

sealed trait Event extends Product with Serializable {
  val id: String
}

object Event {

  final case class ServiceUp(id: String, name: String, useProxy: Boolean, metadata: Map[String, String] = Map.empty) extends Event
  final case class ServiceDown(id: String)                                                                           extends Event
  final case class ServiceContentChanged(id: String)                                                                 extends Event

  implicit val encoder: Encoder[Event] = (event: Event) =>
    event match {
      case ServiceUp(id, name, useProxy, metadata) =>
        Json.obj(
          "event"    -> Json.fromString("serviceUp"),
          "id"       -> Json.fromString(id),
          "name"     -> Json.fromString(name),
          "metadata" -> Json.obj(metadata.view.mapValues(Json.fromString).toSeq: _*),
          "useProxy" -> Json.fromBoolean(useProxy)
        )
      case ServiceDown(id) =>
        Json.obj(
          "event" -> Json.fromString("serviceDown"),
          "id"    -> Json.fromString(id)
        )
      case ServiceContentChanged(id) =>
        Json.obj(
          "event" -> Json.fromString("serviceChanged"),
          "id"    -> Json.fromString(id)
        )

    }

  implicit val listEncoder: Encoder[List[ServiceUp]] = (events: List[ServiceUp]) => Json.arr(events.map(encoder(_)): _*)
}
