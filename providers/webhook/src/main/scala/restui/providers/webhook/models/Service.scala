package restui.providers.webhook.models

final case class Service(name: String, specification: String, metadata: Map[String, String] = Map.empty)
