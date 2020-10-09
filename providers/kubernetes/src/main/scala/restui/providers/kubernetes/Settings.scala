package restui.providers.kubernetes

import com.typesafe.config.Config

import scala.concurrent.duration.FiniteDuration
import scala.jdk.DurationConverters._

final case class Settings(pollingInterval: FiniteDuration, labels: Labels)
final case class Labels(protocol: String,
                        port: String,
                        specificationPath: String,
                        useProxy: String)

// $COVERAGE-OFF$
object Settings {
  private val Namespace = "restui.provider.kubernetes"
  def from(config: Config): Settings = {
    val pollingInterval = config.getDuration(s"$Namespace.polling-interval")
    val port            = config.getString(s"$Namespace.labels.port")
    val specificationPath =
      config.getString(s"$Namespace.labels.specification-path")
    val protocol = config.getString(s"$Namespace.labels.protocol")
    val useProxy = config.getString(s"$Namespace.labels.use-proxy")
    Settings(pollingInterval.toScala,
             Labels(protocol, port, specificationPath, useProxy))
  }
}
