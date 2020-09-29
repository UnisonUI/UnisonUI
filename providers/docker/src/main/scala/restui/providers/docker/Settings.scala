package restui.providers.docker

import com.typesafe.config.Config

final case class Settings(dockerHost: String, labels: Labels)
final case class Labels(serviceName: String, port: String, specificationPath: String, useProxy: String)

// $COVERAGE-OFF$
object Settings {
  private val Namespace = "restui.provider.docker"
  def from(config: Config): Settings = {
    val dockerHost        = config.getString(s"$Namespace.host")
    val serviceName       = config.getString(s"$Namespace.labels.service-name")
    val port              = config.getString(s"$Namespace.labels.port")
    val specificationPath = config.getString(s"$Namespace.labels.specification-path")
    val useProxy          = config.getString(s"$Namespace.labels.useProxy")
    Settings(dockerHost, Labels(serviceName, port, specificationPath, useProxy))
  }
}
