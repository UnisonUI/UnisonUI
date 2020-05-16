package restui.server.http
import io.circe.{Encoder, Json}

object Models {
  sealed trait Event {
    val serviceName: String
    val source: String
  }
  case class Up(serviceName: String, source: String)   extends Event
  case class Down(serviceName: String, source: String) extends Event

  object Event {
    implicit val encoder: Encoder[Event] = Encoder.instance {
      case Up(serviceName, source) =>
        Json.obj(
          "event"       -> Json.fromString("up"),
          "serviceName" -> Json.fromString(serviceName),
          "source"      -> Json.fromString(source)
        )
      case Down(serviceName, source) =>
        Json.obj(
          "event"       -> Json.fromString("down"),
          "serviceName" -> Json.fromString(serviceName),
          "source"      -> Json.fromString(source)
        )
    }
  }
}
