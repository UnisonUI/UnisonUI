package tech.unisonui.providers.webhook.routes

import java.io.File
import java.nio.file.Files

import akka.http.scaladsl.model._
import akka.http.scaladsl.server.{Directives, Route}
import akka.stream.QueueOfferResult
import akka.stream.scaladsl.SourceQueueWithComplete
import cats.syntax.either._
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport
import io.circe.generic.auto._
import tech.unisonui.models.{Metadata, Service => ModelService, ServiceEvent}
import tech.unisonui.protobuf.ProtobufCompiler
import tech.unisonui.protobuf.data.Schema._
import tech.unisonui.providers.webhook.models.Service

import scala.concurrent.{ExecutionContext, Future}
import scala.util.chaining._
import scala.util.control.Exception.allCatch

object Services extends Directives with FailFastCirceSupport {
  def route(queue: SourceQueueWithComplete[ServiceEvent])(implicit
      executionContext: ExecutionContext,
      protobufCompiler: ProtobufCompiler): Route =
    pathPrefix("services") {
      upsertService(queue) ~ path(Segment) { serviceName =>
        deleteService(serviceName, queue)
      }
    }

  private def upsertService(
      queue: SourceQueueWithComplete[ServiceEvent])(implicit
      executionContext: ExecutionContext,
      protobufCompiler: ProtobufCompiler) =
    post {
      entity(as[Service]) {
        _.pipe(transformService(_))
          .fold(
            Future.failed,
            _.pipe(ServiceEvent.ServiceUp)
              .pipe(queue.offer(_))
              .flatMap {
                case QueueOfferResult.Failure(ex) => Future.failed(ex)
                case _                            => Future.successful(StatusCodes.NoContent)
              }
          )
          .pipe(onSuccess(_))(complete(_))
      }
    }

  private def transformService(service: Service)(implicit
      protobufCompiler: ProtobufCompiler): Either[Throwable, ModelService] = {
    val id = s"webhook:${service.name}"
    val combinedMetadata =
      service.metadata ++ Map(Metadata.File     -> service.name,
                              Metadata.Provider -> "webhook")
    service match {
      case Service.OpenApi(name, specification, _) =>
        ModelService
          .OpenApi(id, name, specification, combinedMetadata)
          .asRight[Throwable]
      case Service.Grpc(name, protobuf, servers, _) =>
        for {
          tempFile <- allCatch.either(File.createTempFile("webhook", ".proto"))
          _ <-
            allCatch.either(Files.write(tempFile.toPath, protobuf.getBytes()))
          schema <- tempFile.toPath.toSchema
          _ = tempFile.delete()
        } yield ModelService.Grpc(id, name, schema, servers, combinedMetadata)
    }
  }

  private def deleteService(name: String,
                            queue: SourceQueueWithComplete[ServiceEvent])(
      implicit executionContext: ExecutionContext) =
    delete {
      val id           = s"webhook:$name"
      val serviceEvent = ServiceEvent.ServiceDown(id)
      val response = queue.offer(serviceEvent).flatMap {
        case QueueOfferResult.Failure(ex) => Future.failed(ex)
        case _                            => Future.successful(StatusCodes.NoContent)
      }
      onSuccess(response)(complete(_))
    }
}
