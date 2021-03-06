package tech.unisonui.providers.webhook

import akka.NotUsed
import akka.actor.typed.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, StatusCodes}
import akka.http.scaladsl.server.{Directives, ExceptionHandler}
import akka.stream.OverflowStrategy
import akka.stream.scaladsl.{
  BroadcastHub,
  Keep,
  Source,
  SourceQueueWithComplete
}
import com.typesafe.scalalogging.LazyLogging
import tech.unisonui.models.ServiceEvent
import tech.unisonui.protobuf.ProtobufCompiler
import tech.unisonui.providers.webhook.routes.Services

import scala.concurrent.{ExecutionContext, Future}

object HttpServer extends LazyLogging with Directives {

  private val BufferSize = 0

  def start(interface: String, port: Int)(implicit
      actorSystem: ActorSystem[_],
      protobufCompiler: ProtobufCompiler)
      : Future[Source[ServiceEvent, NotUsed]] = {
    implicit val executionContext: ExecutionContext =
      actorSystem.executionContext
    val (queue, source) =
      Source
        .queue[ServiceEvent](BufferSize, OverflowStrategy.backpressure)
        .toMat(BroadcastHub.sink[ServiceEvent])(Keep.both)
        .run()

    Http().newServerAt(interface, port).bind(routes(queue)).map { binding =>
      val address = binding.localAddress
      logger.info(
        s"Webhook server online at http://${address.getHostName}:${address.getPort}/")
      source
    }
  }

  private val exceptionHandler = ExceptionHandler { case exception =>
    logger.error("Something bad happened", exception)
    complete(
      StatusCodes.InternalServerError -> HttpEntity(
        ContentTypes.`text/plain(UTF-8)`,
        "Something bad happened"))
  }

  private def routes(queue: SourceQueueWithComplete[ServiceEvent])(implicit
      executionContext: ExecutionContext,
      protobufCompiler: ProtobufCompiler) =
    handleExceptions(exceptionHandler) {
      Services.route(queue)
    }
}
