package restui.providers.git.github

import java.io.{PrintWriter, StringWriter}
import java.nio.charset.StandardCharsets

import akka.actor.typed.ActorSystem
import akka.http.scaladsl.model.headers.{Authorization, OAuth2BearerToken}
import akka.http.scaladsl.model.{
  ContentTypes,
  HttpEntity,
  HttpHeader,
  HttpMethods,
  HttpRequest,
  RequestEntity
}
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.scaladsl.{Merge, Source => AkkaSource}
import com.typesafe.scalalogging.LazyLogging
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
import io.circe.Json
import restui.providers.git._
import restui.providers.git.github.data._
import restui.providers.git.settings.GithubSettings

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

final case class GithubClient(settings: GithubSettings,
                              requestExecutor: RequestExecutor)

object GithubClient extends LazyLogging {

  private def graphqlQuery(cursor: Option[String]) =
    s"""${cursor.map(_ => "query($cursor: String!)").getOrElse("")}{
  viewer {
    repositories(${cursor
      .map(_ => "after: $cursor, ")
      .getOrElse("")}first: 100) {
      pageInfo {
        endCursor
        hasNextPage
      }
      nodes {
        nameWithOwner
        url
        defaultBranchRef {
          name
        }
      }
    }
  }
}
"""

  def listRepositories(githubClient: GithubClient)(implicit
      system: ActorSystem[_],
      executionContext: ExecutionContext): Source[Node] =
    graphqlRecursiveSource(githubClient)

  private def graphqlRecursiveSource(githubClient: GithubClient,
                                     cursor: Option[String] = None)(implicit
      system: ActorSystem[_],
      executionContext: ExecutionContext): Source[Node] =
    AkkaSource.future(executeRequest(githubClient, cursor)).flatMapConcat {
      case Error(error) =>
        val errorMessage = error.mkString(", ")
        logger.warn(s"Error while contacting github api: $errorMessage")
        AkkaSource.empty[Node]
      case Repository(nodes, maybeCursor) =>
        val nodesSource = AkkaSource(nodes)
        maybeCursor match {
          case None => nodesSource
          case cursor =>
            AkkaSource.combine(
              nodesSource,
              graphqlRecursiveSource(githubClient, cursor))(Merge(_))
        }
    }

  private def executeRequest(githubClient: GithubClient,
                             cursor: Option[String])(implicit
      system: ActorSystem[_],
      executionContext: ExecutionContext) = {
    val request = createRequest(githubClient.settings, cursor)
    githubClient
      .requestExecutor(request)
      .flatMap { response =>
        response.entity.contentType match {
          case ContentTypes.`application/json` =>
            Unmarshal(response.entity).to[GrahpQL]
          case _ =>
            response.entity.toStrict(5.seconds).map { body =>
              val msg = body.data.decodeString(StandardCharsets.UTF_8)
              Error(List(msg))
            }
        }
      }
      .recover { exception =>
        val sw = new StringWriter()
        val pw = new PrintWriter(sw)
        exception.printStackTrace(pw)
        Error(List(exception.getMessage, sw.toString()))
      }
  }

  private def createRequest(github: GithubSettings,
                            cursor: Option[String]): HttpRequest =
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

  private def authenticationHeader(token: String): HttpHeader =
    Authorization(OAuth2BearerToken(token))
}
