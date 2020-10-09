package restui.server.http

import akka.NotUsed
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.sse.ServerSentEvent
import akka.http.scaladsl.server.{Directives, ExceptionHandler}
import akka.stream.scaladsl.Source
import com.typesafe.scalalogging.LazyLogging
import restui.server.http.routes._
import restui.server.service.ServiceActor

import scala.concurrent.{ExecutionContext, Future}

class HttpServer(private val serviceActorRef: ActorRef[ServiceActor.Message],
                 private val eventsSource: Source[ServerSentEvent, NotUsed])(
    implicit actorSystem: ActorSystem[_])
    extends Directives
    with LazyLogging {

  implicit private val executionContext: ExecutionContext =
    actorSystem.executionContext

  def bind(interface: String, port: Int): Future[Http.ServerBinding] =
    Http().newServerAt(interface, port).bind(routes)

  private val exceptionHandler = ExceptionHandler { case exception =>
    logger.error("Something bad happened", exception)
    complete(
      StatusCodes.InternalServerError -> HttpEntity(
        ContentTypes.`text/plain(UTF-8)`,
        "Something bad happened"))
  }

  private val routes =
    handleExceptions(exceptionHandler) {
      Statics.route ~ Realtime.route(eventsSource) ~ Services.route(
        serviceActorRef) ~ Grpc.route(serviceActorRef) ~ Proxy.route
    }
}
