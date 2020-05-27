package restui.providers.kubernetes

import scala.concurrent.duration.FiniteDuration
import scala.jdk.DurationConverters._

import com.typesafe.config.Config

final case class Settings(pollingInterval: FiniteDuration, labels: Labels)
final case class Labels(protocol: String, port: String, swaggerPath: String)

object Settings {
  private val Namespace = "restui.provider.kubernetes"
  def from(config: Config): Settings = {
    val pollingInterval = config.getDuration(s"$Namespace.polling-interval")
    val port            = config.getString(s"$Namespace.labels.port")
    val swaggerPath     = config.getString(s"$Namespace.labels.swagger-path")
    val protocol        = config.getString(s"$Namespace.labels.protocol")
    Settings(pollingInterval.toScala, Labels(protocol, port, swaggerPath))
  }
}
