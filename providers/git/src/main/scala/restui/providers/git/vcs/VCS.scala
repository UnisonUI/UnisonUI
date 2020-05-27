package restui.providers.git.vcs

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

import akka.actor.ActorSystem
import akka.stream.scaladsl.{Merge, Source => AkkaSource}
import restui.providers.git._
import restui.providers.git.git.Git
import restui.providers.git.git.data.Repository
import restui.providers.git.github.{Github, GithubClient}
import restui.providers.git.settings.{GitSettings, GithubSettings, Settings}
import restui.providers.models.Service

object VCS {
  def source(settings: Settings, requestExecutor: RequestExecutor)(implicit
      actorSystem: ActorSystem,
      executionContext: ExecutionContext): Source[Service] = {
    val vcsSources = settings.vcs.map {
      case settings: GithubSettings =>
        Github.retrieveRepositoriesRegularly(GithubClient(settings, requestExecutor))
      case GitSettings(repositories) => Git.source(repositories)
    }.fold(AkkaSource.empty[Repository]) { (acc, source) =>
      AkkaSource.combine(acc, source)(Merge(_))
    }

    AkkaSource.tick(0.second, settings.cacheDuration, ()).zipLatest(vcsSources).map(_._2).via(Git.flow)
  }
}
