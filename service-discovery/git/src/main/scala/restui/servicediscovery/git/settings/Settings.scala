package restui.servicediscovery.git.settings

import scala.concurrent.duration.FiniteDuration
import scala.jdk.DurationConverters._

import com.typesafe.config.Config

final case class Settings(cacheDuration: FiniteDuration, vcs: List[VCS])
object Settings {
  private val Namespace = "restui.service-discovery.git"
  def from(config: Config): Settings = {
    val namespaceConfig = config.getConfig(Namespace)
    val cacheDuration   = namespaceConfig.getDuration("cache-duration").toScala
    val vcs = if (namespaceConfig.hasPath("vcs")) {
      val vcsConfig = namespaceConfig.getConfig("vcs")
      List(
        GitHub.fromConfig(vcsConfig),
        Git.fromConfig(vcsConfig)
      ).flatten
    } else Nil
    Settings(cacheDuration, vcs)
  }
}
