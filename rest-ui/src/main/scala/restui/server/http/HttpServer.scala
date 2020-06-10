package restui.server.http

import scala.concurrent.{ExecutionContext, Future}

import akka.NotUsed
import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.sse.ServerSentEvent
import akka.http.scaladsl.server.{Directives, ExceptionHandler}
import akka.stream.scaladsl.Source
import com.typesafe.scalalogging.LazyLogging
import restui.server.http.routes._

class HttpServer(private val endpointsActorRef: ActorRef, private val eventsSource: Source[ServerSentEvent, NotUsed])(implicit
    actorSystem: ActorSystem)
    extends Directives
    with LazyLogging {

  implicit private val executionContext: ExecutionContext = actorSystem.dispatcher

  def bind(interface: String, port: Int): Future[Http.ServerBinding] =
    Http().bindAndHandle(routes, interface, port)

  private val exceptionHandler = ExceptionHandler {
    case exception =>
      logger.error("Something bad happened", exception)
      complete(StatusCodes.InternalServerError -> HttpEntity(ContentTypes.`text/plain(UTF-8)`, "Something bad happened"))
  }

  private val routes =
    handleExceptions(exceptionHandler) {
      Statics.route ~ Realtime.route(eventsSource) ~ Services.route(endpointsActorRef)
    }
}
