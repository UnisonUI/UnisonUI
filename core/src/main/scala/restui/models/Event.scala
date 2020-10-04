package restui.models
import io.circe.syntax._
import io.circe.{Encoder, Json}

sealed trait Event extends Product with Serializable {
  val id: String
}

object Event {
  sealed trait Service extends Product with Serializable {
    val id: String
  }

  object Service {
    final case class OpenApi(id: String, name: String, useProxy: Boolean, metadata: Map[String, String] = Map.empty) extends Service
    final case class Grpc(id: String, name: String, metadata: Map[String, String] = Map.empty)                       extends Service

    implicit val encoder: Encoder[Service] = Encoder.instance {
      case OpenApi(id, name, useProxy, metadata) =>
        Json.obj(
          "event"    -> Json.fromString("serviceUp"),
          "id"       -> Json.fromString(id),
          "name"     -> Json.fromString(name),
          "metadata" -> Json.obj(metadata.view.mapValues(Json.fromString).toSeq: _*),
          "useProxy" -> Json.fromBoolean(useProxy),
          "type"     -> Json.fromString("openapi")
        )
      case Grpc(id, name, metadata) =>
        Json.obj(
          "event"    -> Json.fromString("serviceUp"),
          "id"       -> Json.fromString(id),
          "name"     -> Json.fromString(name),
          "metadata" -> Json.obj(metadata.view.mapValues(Json.fromString).toSeq: _*),
          "type"     -> Json.fromString("grpc")
        )
    }
  }

  final case class ServiceUp(service: Service)       extends Event { override val id: String = service.id }
  final case class ServiceDown(id: String)           extends Event
  final case class ServiceContentChanged(id: String) extends Event

  implicit val encoder: Encoder[Event] = (event: Event) =>
    event match {
      case ServiceUp(service: Service) => service.asJson
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
