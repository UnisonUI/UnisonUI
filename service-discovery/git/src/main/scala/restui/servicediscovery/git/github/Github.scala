package restui.servicediscovery.git.github

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

import akka.NotUsed
import akka.actor.ActorSystem
import akka.http.scaladsl.model.Uri
import akka.stream.scaladsl.{Flow => AkkaFlow, Source => AkkaSource}
import com.typesafe.scalalogging.LazyLogging
import restui.servicediscovery.git.git.data.Repository
import restui.servicediscovery.git.github.data.Node
import restui.servicediscovery.git.settings.{GitHub => GithubSetting}
import restui.servicediscovery.git.{Flow, Source}

object Github extends LazyLogging {
  def retrieveRepositoriesRegularly(
      githubClient: GithubClient)(implicit system: ActorSystem, executionContext: ExecutionContext): Source[Repository] =
    AkkaSource
      .tick(0.seconds, githubClient.settings.pollingInterval, githubClient)
      .via(retrieveRepositories.async)
      .mapMaterializedValue(_ => NotUsed)

  private def retrieveRepositories(implicit system: ActorSystem, executionContext: ExecutionContext): Flow[GithubClient, Repository] =
    AkkaFlow[GithubClient].flatMapConcat { client =>
      GithubClient
        .listRepositories(client)
        .map(repos => client.settings -> repos)
    }.map {
      case (GithubSetting(token, _, _, repos), Node(name, url, branch)) =>
        repos
          .find(_.location.isMatching(name))
          .map { repo =>
            logger.debug(s"Matching repo: $name")
            val uri          = Uri(url)
            val uriWithToken = uri.withAuthority(uri.authority.copy(userinfo = token))
            Repository(uriWithToken.toString, branch, repo.swaggerPaths)
          }
    }.collect { case Some(repo) => repo }
}
