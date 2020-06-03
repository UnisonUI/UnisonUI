package restui.providers.git.github

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

import akka.NotUsed
import akka.actor.ActorSystem
import akka.http.scaladsl.model.Uri
import akka.stream.scaladsl.{Flow => AkkaFlow, Source => AkkaSource}
import com.typesafe.scalalogging.LazyLogging
import restui.providers.git.git.data.Repository
import restui.providers.git.github.data.Node
import restui.providers.git.settings.GithubSettings
import restui.providers.git.{Flow, Source}

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
        .map(repositories => client.settings -> repositories)
    }.map {
      case (GithubSettings(token, _, _, repositories), Node(name, url, branch)) =>
        repositories
          .find(_.location.isMatching(name))
          .map { repository =>
            logger.debug(s"Matching repository: $name")
            val uri          = Uri(url)
            val uriWithToken = uri.withAuthority(uri.authority.copy(userinfo = token))
            Repository(uriWithToken.toString, branch, repository.specificationPaths)
          }
    }.collect { case Some(repository) => repository }
}
