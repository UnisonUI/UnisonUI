package restui.models

final case class Service(serviceName: String, file: OpenApiFile, metadata: Map[String, String] = Map.empty)
final case class OpenApiFile(contentType: ContentType, content: String)
