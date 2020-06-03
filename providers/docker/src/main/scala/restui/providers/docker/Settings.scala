package restui.providers.docker

import com.typesafe.config.Config

final case class Settings(dockerHost: String, labels: Labels)
final case class Labels(serviceName: String, port: String, specificationPath: String)

object Settings {
  private val Namespace = "restui.provider.docker"
  def from(config: Config): Settings = {
    val dockerHost        = config.getString(s"$Namespace.host")
    val serviceName       = config.getString(s"$Namespace.labels.service-name")
    val port              = config.getString(s"$Namespace.labels.port")
    val specificationPath = config.getString(s"$Namespace.labels.specification-path")
    Settings(dockerHost, Labels(serviceName, port, specificationPath))
  }
}
