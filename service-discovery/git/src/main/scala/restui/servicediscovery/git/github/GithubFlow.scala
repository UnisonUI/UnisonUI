package restui.servicediscovery.git.github

import java.nio.file.Files

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

import akka.NotUsed
import akka.actor.ActorSystem
import akka.http.scaladsl.model.Uri
import akka.stream.scaladsl.{Flow => AkkaFlow, Source => AkkaSource}
import restui.servicediscovery.git.git.Repo
import restui.servicediscovery.git.github.models.Node
import restui.servicediscovery.git.settings.GitHub
import restui.servicediscovery.git.{Flow, Source}

object GithubFlow {
  def retrieveRepositoriesRegularly(
      githubClient: GithubClient)(implicit system: ActorSystem, executionContext: ExecutionContext): Source[Repo] =
    AkkaSource
      .tick(0.seconds, githubClient.settings.pollingInterval, githubClient)
      .via(retrieveRepositories.async)
      .mapMaterializedValue(_ => NotUsed)

  private def retrieveRepositories(implicit system: ActorSystem, executionContext: ExecutionContext): Flow[GithubClient, Repo] =
    AkkaFlow[GithubClient].flatMapConcat { client =>
      GithubClient
        .listRepositories(client)
        .map(repos => client.settings -> repos)
    }.map {
      case (GitHub(_, token, _, _, repos), Node(name, url, branch)) =>
        repos
          .find(_.location.isMatching(name))
          .map { repo =>
            val uri          = Uri(url)
            val uriWithToken = uri.withAuthority(uri.authority.copy(userinfo = token))
            Repo(uriWithToken.toString, branch, Files.createTempDirectory("restui-git-clone").toFile, repo.swaggerPaths)
          }
    }.collect { case Some(repo) => repo }
}
