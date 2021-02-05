package tech.unisonui.providers.git.settings

import com.typesafe.config.Config

import scala.concurrent.duration.FiniteDuration
import scala.jdk.DurationConverters._

final case class Settings(cacheDuration: FiniteDuration, vcs: List[VCS])
object Settings {
  private val Namespace = "unisonui.provider.git"
  def from(config: Config): Settings = {
    val namespaceConfig = config.getConfig(Namespace)
    val cacheDuration   = namespaceConfig.getDuration("cache-duration").toScala
    val vcsConfig       = namespaceConfig.getConfig("vcs")
    val vcs = List(
      GithubSettings.fromConfig(vcsConfig),
      GitSettings.fromConfig(vcsConfig)
    ).filter(_.repos.nonEmpty)
    Settings(cacheDuration, vcs)
  }
}
