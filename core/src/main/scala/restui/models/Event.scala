package restui.models
import io.circe.{Encoder, Json}

sealed trait Event {
  val serviceName: String
}

object Event {

  final case class ServiceUp(serviceName: String, metadata: Map[String, String] = Map.empty) extends Event
  final case class ServiceDown(serviceName: String)                                          extends Event

  implicit val encoder: Encoder[Event] = (event: Event) =>
    event match {
      case ServiceUp(serviceName, metadata) =>
        Json.obj(
          "event"    -> Json.fromString("serviceUp"),
          "name"     -> Json.fromString(serviceName),
          "metadata" -> Json.obj(metadata.view.mapValues(Json.fromString).toSeq: _*)
        )
      case ServiceDown(serviceName) =>
        Json.obj(
          "event" -> Json.fromString("serviceDown"),
          "name"  -> Json.fromString(serviceName)
        )
    }

  implicit val listEncoder: Encoder[List[ServiceUp]] = (events: List[ServiceUp]) => Json.arr(events.map(encoder(_)): _*)
}
