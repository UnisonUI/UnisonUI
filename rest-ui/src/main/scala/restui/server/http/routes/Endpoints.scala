package restui.server.http.routes

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

import akka.NotUsed
import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.sse.ServerSentEvent
import akka.http.scaladsl.model.{HttpRequest, _}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.unmarshalling.Unmarshaller
import akka.pattern.ask
import akka.stream.scaladsl.Source
import akka.util.Timeout
import io.circe.generic.auto._
import restui.server.http.{Models => HttpModels}
import restui.server.service.EndpointsActor._
import restui.servicediscovery.Models._

object Endpoints {
  implicit val timeout: Timeout = 5.seconds
  import akka.http.scaladsl.marshalling.sse.EventStreamMarshalling._

  def route(endpointsActorRef: ActorRef, eventsSource: Source[ServerSentEvent, NotUsed])(implicit
      executionContext: ExecutionContext,
      actorSystem: ActorSystem): Route =
    sseEndpoint(eventsSource) ~ servicesEndpoint(endpointsActorRef) ~ downloadSwaggerEndpoint(endpointsActorRef)

  private def sseEndpoint(eventsSource: Source[ServerSentEvent, NotUsed]): Route =
    path("events") {
      get(complete(eventsSource))
    }

  private def servicesEndpoint(endpointsActorRef: ActorRef)(implicit executionContext: ExecutionContext): Route =
    path("endpoints") {
      get {
        import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
        val response =
          (endpointsActorRef ? GetAll)
            .mapTo[List[Endpoint]]
            .map(_.map(endpoint => HttpModels.Up(endpoint.serviceName)))
        complete(response)
      }
    }

  private def downloadSwaggerEndpoint(endpointsActorRef: ActorRef)(implicit executionContext: ExecutionContext, actorSystem: ActorSystem) =
    path("service" / Segment) { service =>
      get {
        val response = (endpointsActorRef ? Get(service))
          .mapTo[Option[Endpoint]]
          .flatMap {
            case None => Future(StatusCodes.NotFound -> HttpEntity(ContentTypes.`text/plain(UTF-8)`, s"$service is not registered"))
            case Some(endpoint) =>
              Http()
                .singleRequest(HttpRequest(uri = endpoint.address))
                .flatMap { response =>
                  Unmarshaller.stringUnmarshaller(response.entity)
                }
                .map { response =>
                  val entity = HttpEntity(ContentTypes.`text/plain(UTF-8)`, response)
                  StatusCodes.OK -> entity
                }

          }
        complete(response)
      }
    }
}
