package restui.servicediscovery.docker

import com.typesafe.config.Config

final case class Settings(dockerHost: String, labels: Labels)
final case class Labels(serviceName: String, port: String)

object Settings {
  private val Namespace = "restui.service-discovery.docker"
  def from(config: Config): Settings = {
    val dockerHost  = config.getString(s"$Namespace.host")
    val serviceName = config.getString(s"$Namespace.labels.service-name")
    val port        = config.getString(s"$Namespace.labels.port")
    Settings(dockerHost, Labels(serviceName, port))
  }
}
