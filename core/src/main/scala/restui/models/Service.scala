package restui.models

final case class Service(id: String, name: String, file: OpenApiFile, metadata: Map[String, String] = Map.empty)
final case class OpenApiFile(contentType: ContentType, content: String)
