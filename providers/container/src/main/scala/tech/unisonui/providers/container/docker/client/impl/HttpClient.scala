package tech.unisonui.providers.container.docker.client.impl

import akka.NotUsed
import akka.actor.typed.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpMethods, HttpRequest, HttpResponse, Uri}
import akka.http.scaladsl.settings.ConnectionPoolSettings
import akka.http.scaladsl.unmarshalling.Unmarshaller
import akka.stream.scaladsl.Source
import com.typesafe.scalalogging.LazyLogging
import tech.unisonui.providers.container.docker.client.transport.DockerSock
import tech.unisonui.providers.container.docker.client.{
  HttpClient => HttpClientInterface
}

import scala.concurrent.duration.Duration
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

class HttpClient(private val uri: String)(implicit actorSystem: ActorSystem[_])
    extends HttpClientInterface
    with LazyLogging {

  implicit val executionContext: ExecutionContext = actorSystem.executionContext

  private val settings = ConnectionPoolSettings(actorSystem)

  private val (base, connectionSettings) =
    if (uri.startsWith("unix://"))
      (Uri("http://localhost"),
       settings.withTransport(new DockerSock(uri.replace("unix://", ""))))
    else (Uri(uri.replace("tcp", "http")), settings)

  override def get(path: Uri): Future[HttpResponse] =
    request(
      HttpRequest(uri = path.resolvedAgainst(base), method = HttpMethods.GET))

  override def watch(path: Uri): Source[HttpResponse, NotUsed] =
    Source
      .single(HttpRequest(uri = path.resolvedAgainst(base),
                          method = HttpMethods.GET) -> NotUsed)
      .via(Http().newHostConnectionPool[NotUsed](
        host = base.authority.host.address,
        port = base.authority.port,
        settings = connectionSettings
          .withPipeliningLimit(1)
          .withIdleTimeout(Duration.Inf)
          .withMaxRetries(0)
          .withMaxConnections(1)
          .withMaxConnectionLifetime(Duration.Inf)
      ))
      .mapMaterializedValue(_ => NotUsed)
      .flatMapConcat {
        case (Success(response), _) => Source.single(response)
        case (Failure(e), _) =>
          logger.warn("Error with upstream", e)
          Source.empty[HttpResponse]
      }
  override def downloadFile(uri: String): Future[String] =
    Http()
      .singleRequest(HttpRequest(uri = uri))
      .flatMap { response =>
        Unmarshaller.stringUnmarshaller(response.entity)
      }

  private def request(httpRequest: HttpRequest): Future[HttpResponse] =
    Http().singleRequest(httpRequest, settings = connectionSettings)

}
