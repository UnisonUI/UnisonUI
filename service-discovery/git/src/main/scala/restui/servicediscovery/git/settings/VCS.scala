package restui.servicediscovery.git.settings

import com.typesafe.config.Config
import scala.jdk.CollectionConverters._

sealed trait VCS {
  val repos: Seq[Repo]
}

final case class GitHub(apiToken: String, apiUri: String, override val repos: Seq[Repo]) extends VCS
final case class Git(override val repos: Seq[Repo])                                      extends VCS

object GitHub {
  def fromConfig(config: Config): Option[GitHub] =
    if (config.hasPath("github")) {
      val githubConfig = config.getConfig("github")
      val apiToken     = githubConfig.getString("api-token")
      val apiUri =
        if (githubConfig.hasPath("api-uri")) githubConfig.getString("api-uri")
        else "https://api.github.com"
      val repos = Repo.getListOfRepos(githubConfig, "repos")
      Some(GitHub(apiToken, apiUri, repos))
    } else None
}

// object Gi {
// def fromConfig(config: Config): Option[Git] =
//
// }
