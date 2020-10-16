package tech.unisonui.providers.kubernetes

import cats.syntax.option._
import com.typesafe.config.Config

import scala.concurrent.duration.FiniteDuration
import scala.jdk.DurationConverters._

final case class Settings(pollingInterval: FiniteDuration, labels: Labels)
final case class Labels(protocol: String,
                        port: String,
                        specificationPath: String,
                        useProxy: String,
                        grpcPort: Option[String],
                        grpcTls: String)

// $COVERAGE-OFF$
object Settings {
  private val Namespace = "unisonui.provider.kubernetes"
  def from(config: Config): Settings = {
    val pollingInterval = config.getDuration(s"$Namespace.polling-interval")
    val port            = config.getString(s"$Namespace.labels.port")
    val specificationPath =
      config.getString(s"$Namespace.labels.specification-path")
    val protocol = config.getString(s"$Namespace.labels.protocol")
    val useProxy = config.getString(s"$Namespace.labels.use-proxy")
    val grpcPort = config.getString(s"$Namespace.labels.grpc-port").some
    val grpcTls  = config.getString(s"$Namespace.labels.grpc-tls")
    Settings(
      pollingInterval.toScala,
      Labels(protocol, port, specificationPath, useProxy, grpcPort, grpcTls))
  }
}
