package restui.servicediscovery.git.settings

import scala.concurrent.duration._
import scala.jdk.DurationConverters._

import com.typesafe.config.Config

sealed trait VCS {
  val repos: Seq[Repo]
}

final case class GitHub(apiToken: String, apiUri: String, pollingInterval: FiniteDuration, override val repos: Seq[Repo]) extends VCS
final case class Git(override val repos: Seq[Repo])                                                                       extends VCS

object GitHub {
  def fromConfig(config: Config): Option[GitHub] =
    if (config.hasPath("github")) {
      val githubConfig = config.getConfig("github")
      val apiToken     = githubConfig.getString("api-token")
      val pollingInterval =
        if (githubConfig.hasPath("polling-interval")) githubConfig.getDuration("polling-interval").toScala
        else 1.hours
      val apiUri =
        if (githubConfig.hasPath("api-uri")) githubConfig.getString("api-uri")
        else "https://api.github.com"
      val repos = Repo.getListOfRepos(githubConfig, "repos")
      Some(GitHub(apiToken, apiUri, pollingInterval, repos))
    } else None
}

object Git {
  def fromConfig(config: Config): Option[Git] =
    if (config.hasPath("git")) {
      val repos = Repo.getListOfRepos(config.getConfig("git"), "repos")
      Some(Git(repos))
    } else None

}
