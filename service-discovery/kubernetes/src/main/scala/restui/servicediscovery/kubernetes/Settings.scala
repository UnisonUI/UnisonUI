package restui.servicediscovery.kubernetes

import com.typesafe.config.Config
final case class Settings(labels: Labels)
final case class Labels(protocol: String, port: String, swaggerPath: String)

object Settings {
  private val Namespace = "restui.service-discovery.kubernetes"
  def from(config: Config): Settings = {
    val port        = config.getString(s"$Namespace.labels.port")
    val swaggerPath = config.getString(s"$Namespace.labels.swagger-path")
    val protocol    = config.getString(s"$Namespace.labels.protocol")
    Settings(Labels(protocol, port, swaggerPath))
  }
}
