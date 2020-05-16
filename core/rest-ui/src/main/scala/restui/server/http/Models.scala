package restui.server.http
import io.circe.{Encoder, Json}

object Models {
  sealed trait Event
  case class Up(serviceName: String)   extends Event
  case class Down(serviceName: String) extends Event

  object Event {
    implicit val encoder: Encoder[Event] = Encoder.instance {
      case Up(serviceName) =>
        Json.obj(
          "event"       -> Json.fromString("up"),
          "serviceName" -> Json.fromString(serviceName)
        )
      case Down(serviceName) =>
        Json.obj(
          "event"       -> Json.fromString("down"),
          "serviceName" -> Json.fromString(serviceName)
        )
    }
  }
}
