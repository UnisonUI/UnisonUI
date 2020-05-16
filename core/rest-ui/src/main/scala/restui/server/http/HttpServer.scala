package restui.server.http

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContextExecutor, Future}

import akka.NotUsed
import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.sse.ServerSentEvent
import akka.http.scaladsl.model.{HttpRequest, _}
import akka.http.scaladsl.server.{Directives, ExceptionHandler}
import akka.http.scaladsl.unmarshalling.Unmarshaller
import akka.pattern.ask
import akka.stream.scaladsl.Source
import akka.util.Timeout
import io.circe.generic.auto._
import org.slf4j.LoggerFactory
import restui.server.http.{Models => HttpModels}
import restui.server.service.EndpointsActor._
import restui.servicediscovery.Models._

class HttpServer(private val endpointsActorRef: ActorRef, private val eventsSource: Source[ServerSentEvent, NotUsed])(implicit
    actorSystem: ActorSystem)
    extends Directives {
  import akka.http.scaladsl.marshalling.sse.EventStreamMarshalling._
  private val logger                                              = LoggerFactory.getLogger(classOf[HttpServer])
  implicit val timeout: Timeout                                   = 5.seconds
  implicit private val executionContext: ExecutionContextExecutor = actorSystem.dispatcher

  def bind(port: Int): Future[Http.ServerBinding] =
    Http().bindAndHandle(routes, "0.0.0.0", port)

  private val routes =
    handleExceptions(ExceptionHandler {
      case e =>
        logger.error("Exception", e)
        complete(StatusCodes.InternalServerError -> HttpEntity(ContentTypes.`text/plain(UTF-8)`, "Something bas happened"))
    }) {
      path("events") {
        get(complete(eventsSource))
      } ~ pathPrefix("statics")(getFromResourceDirectory("web/statics")) ~
        path(PathEnd) {
          getFromResource("web/index.html")
        } ~
        path("endpoints") {
          get {
            import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
            val response =
              (endpointsActorRef ? GetAll)
                .mapTo[List[(String, Endpoint)]]
                .map(_.map { case (source, endpoint) => HttpModels.Up(endpoint.serviceName, source) })
            complete(response)
          }
        } ~ path("service" / Segment) { service =>
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
}
