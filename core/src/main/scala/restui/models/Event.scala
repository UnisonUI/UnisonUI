package restui.models
import io.circe.syntax._
import io.circe.{Encoder, Json}

sealed trait Type
object Type {
  case object OpenApi extends Type
  case object Grpc    extends Type
  implicit val encoder: Encoder[Type] = (serviceType: Type) =>
    serviceType match {
      case OpenApi => Json.fromString("openapi")
      case Grpc    => Json.fromString("grpc")
    }
}

sealed trait Event extends Product with Serializable {
  val id: String
}

object Event {

  final case class ServiceUp(id: String,
                             name: String,
                             useProxy: Boolean,
                             metadata: Map[String, String] = Map.empty,
                             serviceType: Type = Type.OpenApi)
      extends Event
  final case class ServiceDown(id: String)           extends Event
  final case class ServiceContentChanged(id: String) extends Event

  implicit val encoder: Encoder[Event] = (event: Event) =>
    event match {
      case ServiceUp(id, name, useProxy, metadata, serviceType) =>
        Json.obj(
          "event"    -> Json.fromString("serviceUp"),
          "id"       -> Json.fromString(id),
          "name"     -> Json.fromString(name),
          "metadata" -> Json.obj(metadata.view.mapValues(Json.fromString).toSeq: _*),
          "useProxy" -> Json.fromBoolean(useProxy),
          "type"     -> serviceType.asJson
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
