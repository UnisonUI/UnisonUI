package restui.servicediscovery.git.vcs

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

import akka.actor.ActorSystem
import akka.stream.scaladsl.{Merge, Source => AkkaSource}
import restui.servicediscovery.git._
import restui.servicediscovery.git.git.Git
import restui.servicediscovery.git.git.data.Repository
import restui.servicediscovery.git.github.{Github, GithubClient}
import restui.servicediscovery.git.settings.{Git => GitSetting, GitHub => GithubSetting, Settings}
import restui.servicediscovery.models.Service

object VCS {
  def source(settings: Settings, requestExecutor: RequestExecutor)(implicit
      actorSystem: ActorSystem,
      executionContext: ExecutionContext): Source[Service] = {
    val vcsSources = settings.vcs.map {
      case settings: GithubSetting =>
        Github.retrieveRepositoriesRegularly(GithubClient(settings, requestExecutor))
      case GitSetting(repositories) => Git.source(repositories)
    }.fold(AkkaSource.empty[Repository]) { (acc, source) =>
      AkkaSource.combine(acc, source)(Merge(_))
    }

    AkkaSource.tick(0.second, settings.cacheDuration, ()).zipLatest(vcsSources).map(_._2).via(Git.flow)
  }
}
