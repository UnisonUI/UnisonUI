package restui.servicediscovery.git.github

import java.nio.file.Files

import scala.concurrent.ExecutionContext

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.scaladsl.Flow
import restui.Concurrency
import restui.servicediscovery.git.git.Repo
import restui.servicediscovery.git.github.models.Node
import restui.servicediscovery.git.settings.GitHub

object GithubFlow {

  def flow(implicit system: ActorSystem, executionContext: ExecutionContext): Flow[GithubClient, Repo, NotUsed] =
    Flow[GithubClient]
      .flatMapMerge(Concurrency.AvailableCore,
                    client =>
                      GithubClient
                        .listRepositories(client)
                        .map(repos => client.settings -> repos)
                        .async)
      .map {
        case (GitHub(_, token, _, _, repos), Node(name, url, branch)) =>
          repos
            .find(_.location.isMatching(name))
            .map { repo =>
              Repo(url, branch, Files.createTempDirectory("restui-git-clone").toFile, repo.swaggerPaths)
            }
      }
      .collect { case Some(repo) => repo }
      .async
}
