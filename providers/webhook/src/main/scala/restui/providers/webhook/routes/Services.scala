package restui.providers.webhook.routes

import scala.concurrent.{ExecutionContext, Future}

import akka.http.scaladsl.model._
import akka.http.scaladsl.server.{Directives, Route}
import akka.stream.QueueOfferResult
import akka.stream.scaladsl.SourceQueueWithComplete
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport
import io.circe.generic.auto._
import restui.models.{Metadata, Service => ModelService, ServiceEvent}
import restui.providers.webhook.models.Service

object Services extends Directives with FailFastCirceSupport {
  def route(queue: SourceQueueWithComplete[ServiceEvent])(implicit executionContext: ExecutionContext): Route =
    path("services") {
      upsertService(queue) ~ get {
        complete(StatusCodes.OK)
      }
    }
  private def upsertService(queue: SourceQueueWithComplete[ServiceEvent])(implicit executionContext: ExecutionContext) =
    post {
      entity(as[Service]) { service =>
        import service._
        val id = s"webhook:${name}"
        val serviceEvent = ServiceEvent.ServiceUp(
          ModelService(id, name, specification, metadata ++ Map(Metadata.File -> name, Metadata.Provider -> "webhook")))
        val response = queue.offer(serviceEvent).flatMap {
          case QueueOfferResult.Enqueued    => Future.successful(StatusCodes.NoContent)
          case QueueOfferResult.Failure(ex) => Future.failed(ex)
          case _                            => Future.successful(StatusCodes.NoContent)
        }
        onSuccess(response)(complete(_))
      }
    }
}
