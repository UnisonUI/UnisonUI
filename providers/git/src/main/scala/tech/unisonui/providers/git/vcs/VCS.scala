package tech.unisonui.providers.git.vcs

import akka.actor.typed.ActorSystem
import akka.stream.scaladsl.{Merge, Source => AkkaSource}
import tech.unisonui.models.ServiceEvent
import tech.unisonui.protobuf.ProtobufCompiler
import tech.unisonui.providers.git._
import tech.unisonui.providers.git.git.Git
import tech.unisonui.providers.git.github.{Github, GithubClient}
import tech.unisonui.providers.git.settings.{
  GitSettings,
  GithubSettings,
  Settings
}

import scala.concurrent.ExecutionContext

object VCS {
  def source(settings: Settings, requestExecutor: RequestExecutor)(implicit
      actorSystem: ActorSystem[_],
      executionContext: ExecutionContext,
      protobufCompiler: ProtobufCompiler): Source[ServiceEvent] =
    settings.vcs.map {
      case githubSettings: GithubSettings =>
        Git.fromSource(settings.cacheDuration,
                       Github(GithubClient(githubSettings, requestExecutor)))
      case GitSettings(repositories) =>
        Git.fromSettings(settings.cacheDuration, repositories)
    }.fold(AkkaSource.empty[ServiceEvent]) { (acc, source) =>
      AkkaSource.combine(acc, source)(Merge(_))
    }
}
