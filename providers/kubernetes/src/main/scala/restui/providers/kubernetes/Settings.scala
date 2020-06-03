package restui.providers.kubernetes

import scala.concurrent.duration.FiniteDuration
import scala.jdk.DurationConverters._

import com.typesafe.config.Config

final case class Settings(pollingInterval: FiniteDuration, labels: Labels)
final case class Labels(protocol: String, port: String, specificationPath: String)

object Settings {
  private val Namespace = "restui.provider.kubernetes"
  def from(config: Config): Settings = {
    val pollingInterval   = config.getDuration(s"$Namespace.polling-interval")
    val port              = config.getString(s"$Namespace.labels.port")
    val specificationPath = config.getString(s"$Namespace.labels.specification-path")
    val protocol          = config.getString(s"$Namespace.labels.protocol")
    Settings(pollingInterval.toScala, Labels(protocol, port, specificationPath))
  }
}
