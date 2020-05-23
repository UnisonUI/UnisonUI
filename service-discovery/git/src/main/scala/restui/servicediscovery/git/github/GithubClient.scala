package restui.servicediscovery.git.github

import scala.concurrent.ExecutionContext

import akka.NotUsed
import akka.actor.ActorSystem
import akka.http.scaladsl.model.headers.{Authorization, OAuth2BearerToken}
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpHeader, HttpMethods, HttpRequest}
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.scaladsl.{Merge, Source}
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
import io.circe.Json
import org.slf4j.LoggerFactory
import restui.servicediscovery.git.RequestExecutor
import restui.servicediscovery.git.settings.GitHub

final case class GithubClient(settings: GitHub, requestExecutor: RequestExecutor)

object GithubClient {

  private val logger              = LoggerFactory.getLogger(GithubClient.getClass)
  private val QueryWithPagination = """query($cursor: String!){
  viewer {
    repositories(after: $cursor, first: 100) {
      pageInfo {
        endCursor
        hasNextPage
      }
      nodes {
        nameWithOwner
        projectsUrl
      }
    }
  }
  rateLimit {
    cost
    remaining
    resetAt
    limit
  }
}
"""

  private val QueryWithoutPagination = """{
  viewer {
    repositories(first: 100) {
      pageInfo {
        endCursor
        hasNextPage
      }
      nodes {
        nameWithOwner
        projectsUrl
      }
    }
  }
}
"""

  final case class Repository(name: String, url: String)

  def listRepositories(githubClient: GithubClient)(implicit system: ActorSystem, executionContext: ExecutionContext) =
    graphqlRecursiveSource(githubClient)

  private def graphqlRecursiveSource(githubClient: GithubClient, cursor: Option[String] = None)(implicit
      system: ActorSystem,
      executionContext: ExecutionContext): Source[Repository, NotUsed] =
    Source.future(executeRequest(githubClient, cursor)).map(parseGraphqlResponse).flatMapConcat {
      case Left(error) =>
        logger.warn("Error while contacting github api: {}", error)
        Source.empty[Repository]
      case Right((maybeCursor, nodes)) =>
        val nodesSource = Source(nodes)
        maybeCursor match {
          case None   => nodesSource
          case cursor => Source.combine(nodesSource, graphqlRecursiveSource(githubClient, cursor))(Merge(_))
        }
    }

  private def parseGraphqlResponse(json: Json) =
    json.hcursor.downField("errors").focus.flatMap(_.asArray).map(_.flatMap(_.hcursor.get[String]("message").toOption)) match {
      case Some(messages) => Left(messages.mkString(", "))
      case None =>
        val hcursor  = json.hcursor.downField("data").downField("viewer").downField("repositories")
        val pageInfo = hcursor.downField("pageInfo")
        val cursor =
          if (pageInfo.get[Boolean]("hasNextPage").getOrElse(false)) pageInfo.get[String]("endCursor").toOption
          else None
        val nodes = hcursor
          .downField("nodes")
          .focus
          .flatMap(_.asArray)
          .getOrElse(Vector.empty)
          .flatMap { json =>
            val cursor = json.hcursor
            for {
              name <- cursor.get[String]("nameWithOwner").toOption
              url  <- cursor.get[String]("projectsUrl").toOption
            } yield Repository(name, url)
          }
        Right(cursor -> nodes)
    }

  private def executeRequest(githubClient: GithubClient, cursor: Option[String])(implicit
      system: ActorSystem,
      executionContext: ExecutionContext) = {
    val request = createRequest(githubClient.settings, cursor)
    githubClient.requestExecutor(request).flatMap { response =>
      Unmarshal(response.entity).to[Json]
    }
  }

  private def createRequest(github: GitHub, cursor: Option[String]): HttpRequest =
    HttpRequest(
      uri = github.apiUri,
      method = HttpMethods.POST,
      headers = authenticationHeader(github.apiToken) :: Nil,
      entity = createGraphqlBody(cursor)
    )

  private def createGraphqlBody(maybeCursor: Option[String]) = {
    val json = maybeCursor match {
      case None => Json.obj("query" -> Json.fromString(QueryWithoutPagination))
      case Some(cursor) =>
        Json.obj("query" -> Json.fromString(QueryWithPagination), "variables" -> Json.obj("cursor" -> Json.fromString(cursor)))
    }
    HttpEntity(ContentTypes.`application/json`, json.noSpaces)
  }

  private def authenticationHeader(token: String): HttpHeader = Authorization(OAuth2BearerToken(token))
}
