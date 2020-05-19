package restui.server.http

import scala.concurrent.{ExecutionContext, Future}

import akka.NotUsed
import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.sse.ServerSentEvent
import akka.http.scaladsl.server.{Directives, ExceptionHandler}
import akka.stream.scaladsl.Source
import org.slf4j.LoggerFactory
import restui.server.http.routes._

class HttpServer(private val endpointsActorRef: ActorRef, private val eventsSource: Source[ServerSentEvent, NotUsed])(implicit
    actorSystem: ActorSystem)
    extends Directives {
  private val logger                                      = LoggerFactory.getLogger(classOf[HttpServer])
  implicit private val executionContext: ExecutionContext = actorSystem.dispatcher

  def bind(interface: String, port: Int): Future[Http.ServerBinding] =
    Http().bindAndHandle(routes, interface, port)

  private val routes =
    handleExceptions(ExceptionHandler {
      case e =>
        logger.error("Exception", e)
        complete(StatusCodes.InternalServerError -> HttpEntity(ContentTypes.`text/plain(UTF-8)`, "Something bas happened"))
    }) {
      Statics.route ~ Endpoints.route(endpointsActorRef, eventsSource)
    }
}
