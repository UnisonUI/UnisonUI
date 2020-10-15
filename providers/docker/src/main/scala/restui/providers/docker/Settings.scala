package restui.providers.docker

import cats.syntax.option._
import com.typesafe.config.Config

final case class Settings(dockerHost: String, labels: Labels)
final case class Labels(serviceName: String,
                        port: String,
                        specificationPath: String,
                        useProxy: String,
                        grpcPort: Option[String],
                        grpcTls: String)

// $COVERAGE-OFF$
object Settings {
  private val Namespace = "restui.provider.docker"
  def from(config: Config): Settings = {
    val dockerHost  = config.getString(s"$Namespace.host")
    val serviceName = config.getString(s"$Namespace.labels.service-name")
    val port        = config.getString(s"$Namespace.labels.port")
    val specificationPath =
      config.getString(s"$Namespace.labels.specification-path")
    val useProxy = config.getString(s"$Namespace.labels.use-proxy")
    val grpcPort = config.getString(s"$Namespace.labels.grpc-port").some
    val grpcTls  = config.getString(s"$Namespace.labels.grpc-tls")

    Settings(
      dockerHost,
      Labels(serviceName, port, specificationPath, useProxy, grpcPort, grpcTls))
  }
}
