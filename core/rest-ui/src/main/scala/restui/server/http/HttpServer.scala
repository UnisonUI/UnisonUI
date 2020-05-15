package restui.server.http

import akka.actor.{ActorRef, ActorSystem}
import akka.pattern.ask
import akka.util.Timeout

import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport
import io.circe.generic.auto._
import restui.server.http.templates._
import restui.server.service.EndpointsActor.Get
import restui.servicediscovery.Models.Endpoint
import scala.concurrent.duration._
import scala.concurrent.{ExecutionContextExecutor, Future}

import java.util.Base64
import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.Unmarshaller

class HttpServer(private val endpointsActorRef: ActorRef)(implicit actorSystem: ActorSystem)
    extends Directives
    with FailFastCirceSupport
    with ScalaTagsSupport {
  implicit val timeout: Timeout                                   = 5.seconds
  implicit private val executionContext: ExecutionContextExecutor = actorSystem.dispatcher

  def bind(port: Int): Future[Http.ServerBinding] =
    Http().bindAndHandle(routes, "0.0.0.0", port)

  private val routes =
    pathPrefix("statics")(getFromResourceDirectory("web")) ~ path(PathEnd) {
      extractRequestContext { implicit context =>
        val html = (endpointsActorRef ? Get).mapTo[List[Endpoint]].map(Html.template)
        get(complete(html))
      }
    } ~
      path("endpoints") {
        get {
          val response = (endpointsActorRef ? Get).mapTo[List[Endpoint]]
          complete(response)
        }
      } ~ path(Remaining) { url =>
      get {
        val uri = new String(Base64.getDecoder.decode(url.getBytes))
        val response = Http()
          .singleRequest(HttpRequest(uri = uri))
          .flatMap { response =>
            Unmarshaller.stringUnmarshaller(response.entity)
          }
          .map { response =>
            val entity = HttpEntity(ContentTypes.`text/plain(UTF-8)`, response)
            StatusCodes.OK -> entity
          }
        complete(response)
      }
    }
}
