package restui.providers

package object models {
  final case class Service(serviceName: String, file: OpenApiFile, metadata: Map[String, String] = Map.empty)
  final case class OpenApiFile(contentType: ContentType, content: String)

  sealed trait ContentType
  object ContentType {
    def fromString(string: String): ContentType =
      if (string.endsWith("yaml") || string.endsWith("yml")) ContentTypes.Yaml
      else if (string.endsWith("json")) ContentTypes.Json
      else ContentTypes.Plain
  }
  object ContentTypes {
    case object Plain extends ContentType
    case object Json  extends ContentType
    case object Yaml  extends ContentType
  }

  sealed trait ServiceEvent
  final case class ServiceUp(service: Service)      extends ServiceEvent
  final case class ServiceDown(serviceName: String) extends ServiceEvent
}
