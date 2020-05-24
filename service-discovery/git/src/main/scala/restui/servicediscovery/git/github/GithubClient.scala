package restui.servicediscovery.git.github

import scala.concurrent.ExecutionContext

import akka.actor.ActorSystem
import akka.http.scaladsl.model.headers.{Authorization, OAuth2BearerToken}
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpHeader, HttpMethods, HttpRequest, RequestEntity}
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.scaladsl.{Merge, Source => AkkaSource}
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
import io.circe.Json
import org.slf4j.LoggerFactory
import restui.servicediscovery.git.github.models._
import restui.servicediscovery.git.settings.GitHub
import restui.servicediscovery.git.{RequestExecutor, _}

final case class GithubClient(settings: GitHub, requestExecutor: RequestExecutor)

object GithubClient {

  private val logger                               = LoggerFactory.getLogger(GithubClient.getClass)
  private def graphqlQuery(cursor: Option[String]) = s"""${cursor.map(_ => "query($cursor: String!)").getOrElse("")}{
  viewer {
    repositories(${cursor.map(_ => "after: $cursor, ").getOrElse("")}first: 100) {
      pageInfo {
        endCursor
        hasNextPage
      }
      nodes {
        nameWithOwner
        projectsUrl
        defaultBranchRef {
          name
        }
      }
    }
  }
}
"""

  def listRepositories(githubClient: GithubClient)(implicit system: ActorSystem, executionContext: ExecutionContext): Source[Node] =
    graphqlRecursiveSource(githubClient)

  private def graphqlRecursiveSource(githubClient: GithubClient, cursor: Option[String] = None)(implicit
      system: ActorSystem,
      executionContext: ExecutionContext): Source[Node] =
    AkkaSource.future(executeRequest(githubClient, cursor)).flatMapConcat {
      case Error(error) =>
        logger.warn("Error while contacting github api: {}", error)
        AkkaSource.empty[Node]
      case Repository(nodes, maybeCursor) =>
        val nodesSource = AkkaSource(nodes)
        maybeCursor match {
          case None   => nodesSource
          case cursor => AkkaSource.combine(nodesSource, graphqlRecursiveSource(githubClient, cursor))(Merge(_))
        }
    }

  private def executeRequest(githubClient: GithubClient, cursor: Option[String])(implicit
      system: ActorSystem,
      executionContext: ExecutionContext) = {
    val request = createRequest(githubClient.settings, cursor)
    githubClient
      .requestExecutor(request)
      .flatMap { response =>
        Unmarshal(response.entity).to[GrahpQL]
      }
      .recover(exception => Error(List(exception.getMessage)))
  }

  private def createRequest(github: GitHub, cursor: Option[String]): HttpRequest =
    HttpRequest(
      uri = github.apiUri,
      method = HttpMethods.POST,
      headers = authenticationHeader(github.apiToken) :: Nil,
      entity = createGraphqlBody(cursor)
    )

  private def createGraphqlBody(maybeCursor: Option[String]): RequestEntity = {
    val query = Json.fromString(graphqlQuery(maybeCursor))
    val json = maybeCursor.fold(Json.obj("query" -> query)) { cursor =>
      Json.obj(
        "query"     -> query,
        "variables" -> Json.obj("cursor" -> Json.fromString(cursor))
      )
    }
    HttpEntity(ContentTypes.`application/json`, json.noSpaces)
  }

  private def authenticationHeader(token: String): HttpHeader = Authorization(OAuth2BearerToken(token))
}
