package restui.providers.git.github

import scala.concurrent.ExecutionContext

import akka.actor.ActorSystem
import akka.http.scaladsl.model.Uri
import akka.stream.SourceShape
import akka.stream.scaladsl.{Broadcast, Flow => AkkaFlow, GraphDSL, Merge, Source => AkkaSource}
import com.typesafe.scalalogging.LazyLogging
import restui.Concurrency
import restui.providers.git.Source
import restui.providers.git.git.data.{Repository, UnnamedSpecification}
import restui.providers.git.github.data.Node

object Github extends LazyLogging {
  def apply(githubClient: GithubClient)(implicit system: ActorSystem, executionContext: ExecutionContext): Source[Repository] =
    AkkaSource.fromGraph(GraphDSL.create() { implicit builder =>
      import GraphDSL.Implicits._
      val init = builder.add(AkkaSource.single(List.empty[String]))

      val outbound = builder.add(
        AkkaFlow[(List[String], List[Node])].flatMapMerge(
          Concurrency.AvailableCore,
          {
            case (oldRepositories, newRepositories) =>
              val repositories = newRepositories.collect {
                case Node(name, url, branch) if !oldRepositories.contains(name) =>
                  githubClient.settings.repos.find(_.location.isMatching(name)).map { repository =>
                    logger.debug(s"Matching repository: $name")
                    val uri          = Uri(url)
                    val uriWithToken = uri.withAuthority(uri.authority.copy(userinfo = githubClient.settings.apiToken))
                    Repository(uriWithToken.toString,
                               branch,
                               repository.specificationPaths.map(UnnamedSpecification(_)),
                               useProxy = repository.useProxy)
                  }

              }.collect { case Some(repository) => repository }
              AkkaSource(repositories)
          }
        ))

      val retrieveRepositories = builder.add(AkkaFlow[List[String]].flatMapConcat { old =>
        GithubClient
          .listRepositories(githubClient)
          .fold(List.empty[Node])(_ :+ _)
          .map(repositories => old -> repositories)
          .async
      })

      val merge = builder.add(Merge[List[String]](2))

      val delay = builder.add(AkkaFlow[(List[String], List[Node])].delay(githubClient.settings.pollingInterval).map {
        case (_, repositories) => repositories.map(_.name)
      })

      val broadcast = builder.add(Broadcast[(List[String], List[Node])](2, eagerCancel = true))
      // format: OFF
      init ~> merge ~> retrieveRepositories ~> broadcast ~> outbound
              merge <~ delay <~ broadcast
      // format: ON
      SourceShape(outbound.out)
    })
}
