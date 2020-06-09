package restui.providers.webhook

import scala.concurrent.{ExecutionContext, Future}

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, StatusCodes}
import akka.http.scaladsl.server.{Directives, ExceptionHandler}
import akka.stream.OverflowStrategy
import akka.stream.scaladsl.{Keep, Sink, Source, SourceQueueWithComplete}
import com.typesafe.scalalogging.LazyLogging
import restui.models.ServiceEvent
import restui.providers.Provider
import restui.providers.webhook.routes.Services

object HttpServer extends LazyLogging with Directives {

  private val BufferSize = 0

  def bind(interface: String, port: Int, callback: Provider.Callback)(implicit actorSystem: ActorSystem): Future[Http.ServerBinding] = {
    implicit val executionContext: ExecutionContext = actorSystem.dispatcher
    val queue                                       = Source.queue[ServiceEvent](BufferSize, OverflowStrategy.backpressure).toMat(Sink.foreach(callback))(Keep.left).run()
    Http().bindAndHandle(routes(queue), interface, port)
  }

  private val exceptionHandler = ExceptionHandler {
    case exception =>
      logger.error("Something bad happened", exception)
      complete(StatusCodes.InternalServerError -> HttpEntity(ContentTypes.`text/plain(UTF-8)`, "Something bad happened"))
  }

  private def routes(queue: SourceQueueWithComplete[ServiceEvent])(implicit executionContext: ExecutionContext) =
    handleExceptions(exceptionHandler) {
      Services.route(queue)
    }
}
