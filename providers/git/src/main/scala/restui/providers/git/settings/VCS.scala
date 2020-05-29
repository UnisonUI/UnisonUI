package restui.providers.git.settings

import scala.concurrent.duration._
import scala.jdk.DurationConverters._

import com.typesafe.config.Config

sealed trait VCS {
  val repos: Seq[RepositorySettings]
}

final case class GithubSettings(apiToken: String,
                                apiUri: String = "https://api.github.com/graphql",
                                pollingInterval: FiniteDuration = 1.hour,
                                override val repos: Seq[RepositorySettings] = Nil)
    extends VCS

final case class GitSettings(override val repos: Seq[RepositorySettings]) extends VCS

object GithubSettings {
  def fromConfig(config: Config): GithubSettings = {
    val githubConfig    = config.getConfig("github")
    val apiToken        = githubConfig.getString("api-token")
    val pollingInterval = githubConfig.getDuration("polling-interval").toScala
    val apiUri          = githubConfig.getString("api-uri")
    val repos           = RepositorySettings.getListOfRepositories(githubConfig, "repositories")
    GithubSettings(apiToken, apiUri, pollingInterval, repos)
  }
}

object GitSettings {
  def fromConfig(config: Config): GitSettings = {
    val repos = RepositorySettings.getListOfRepositories(config.getConfig("git"), "repositories")
    GitSettings(repos)
  }
}