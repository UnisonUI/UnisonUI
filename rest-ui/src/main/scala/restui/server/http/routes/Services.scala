package restui.server.http.routes

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

import akka.actor.ActorRef
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.pattern.ask
import akka.util.Timeout
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
import restui.providers.models.{ContentTypes => ServiceContentTypes, _}
import restui.server.http.Models
import restui.server.http.Models.Event._
import restui.server.service.ServiceActor._

object Services {
  implicit val timeout: Timeout = 5.seconds

  def route(serviceActorRef: ActorRef)(implicit executionContext: ExecutionContext): Route =
    path("services") {
      get {
        val response =
          (serviceActorRef ? GetAll)
            .mapTo[List[Service]]
            .map(_.map { case Service(name, _, metadata) => Models.ServiceUp(name, metadata) })
        complete(response)
      }
    } ~ path("services" / Remaining) { service =>
      get {
        val response = (serviceActorRef ? Get(service))
          .mapTo[Option[Service]]
          .map {
            case None => StatusCodes.NotFound -> HttpEntity(ContentTypes.`text/plain(UTF-8)`, s"$service is not registered")
            case Some(Service(_, OpenApiFile(contenType, content), _)) =>
              val httpContentType = contenType match {
                case ServiceContentTypes.Json  => ContentTypes.`application/json`
                case ServiceContentTypes.Yaml  => ContentTypes.`text/plain(UTF-8)`
                case ServiceContentTypes.Plain => ContentTypes.`text/plain(UTF-8)`
              }
              val entity = HttpEntity(httpContentType, content)
              StatusCodes.OK -> entity
          }
        complete(response)
      }
    }

}
