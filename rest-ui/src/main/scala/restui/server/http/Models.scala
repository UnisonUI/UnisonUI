package restui.server.http
import io.circe.{Encoder, Json}

object Models {

  sealed trait Event {
    val serviceName: String
  }

  case class ServiceUp(serviceName: String, metadata: Map[String, String] = Map.empty) extends Event
  case class ServiceDown(serviceName: String)                                          extends Event

  object Event {

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
}
