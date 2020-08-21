package restui.providers.webhook

import scala.concurrent.{ExecutionContext, Future}

import akka.NotUsed
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, StatusCodes}
import akka.http.scaladsl.server.{Directives, ExceptionHandler}
import akka.stream.OverflowStrategy
import akka.stream.scaladsl.{BroadcastHub, Keep, Source, SourceQueueWithComplete}
import com.typesafe.scalalogging.LazyLogging
import restui.models.ServiceEvent
import restui.providers.webhook.routes.Services

object HttpServer extends LazyLogging with Directives {

  private val BufferSize = 0

  def start(interface: String, port: Int)(implicit actorSystem: ActorSystem): Future[Source[ServiceEvent, NotUsed]] = {
    implicit val executionContext: ExecutionContext = actorSystem.dispatcher
    val (queue, source) =
      Source.queue[ServiceEvent](BufferSize, OverflowStrategy.backpressure).toMat(BroadcastHub.sink[ServiceEvent])(Keep.both).run()

    Http().newServerAt(interface, port).bind(routes(queue)).map { binding =>
      val address = binding.localAddress
      logger.info(s"Webhook server online at http://${address.getHostName}:${address.getPort}/")
      source
    }
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
