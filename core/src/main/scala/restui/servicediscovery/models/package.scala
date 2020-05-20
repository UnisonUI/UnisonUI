package restui.servicediscovery

package object models {
  final case class Service(serviceName: String, file: OpenApiFile, metadata: Map[String, String] = Map.empty)
  final case class OpenApiFile(contentType: ContentType, content: String)

  sealed trait ContentType

  object ContentTypes {
    case object Plain extends ContentType
    case object Json  extends ContentType
    case object Yaml  extends ContentType
  }

  sealed trait ServiceEvent
  final case class ServiceUp(service: Service)      extends ServiceEvent
  final case class ServiceDown(serviceName: String) extends ServiceEvent
}
