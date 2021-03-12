package tech.unisonui.providers.container.actors

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorSystem, Behavior}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.unmarshalling.Unmarshaller
import akka.stream.scaladsl.SourceQueueWithComplete
import com.typesafe.scalalogging.LazyLogging
import skuber.{Service => KubernetesService}
import tech.unisonui.grpc.ReflectionClient
import tech.unisonui.models._
import tech.unisonui.providers.container.settings._

import scala.concurrent.{ExecutionContext, Future}
import scala.util.chaining._
import scala.util.control.Exception.allCatch

object KubernetesServices extends LazyLogging {
  private final case class OpenApiService(address: String,
                                          specificationPath: String,
                                          useProxyValue: Boolean)
  private final case class ServiceFoundWithFile(
      id: String,
      service: String,
      openApi: Option[OpenApiService],
      grpc: Option[Service.Grpc.Server])
  private val Namespace = "namespace"

  sealed trait Message

  object Message {
    final case class Add(namespace: String, services: List[KubernetesService])
        extends Message
  }

  def apply(settingsLabels: Labels,
            queue: SourceQueueWithComplete[ServiceEvent],
            servicesByNamespaces: Map[String, List[KubernetesService]] =
              Map.empty)(implicit
      system: ActorSystem[_],
      executionContext: ExecutionContext,
      reflectionClient: ReflectionClient): Behavior[Message] =
    Behaviors.receive { (_, message) =>
      message match {
        case Message.Add(namespace, newServices) =>
          val filteredServices = newServices.filter(service =>
            settingsLabels.extractLabels(service.metadata.labels).isDefined)
          servicesByNamespaces
            .get(namespace)
            .fold(filteredServices) { services =>
              services
                .filter(service => !filteredServices.contains(service))
                .flatMap(createEndpoint(settingsLabels, _))
                .foreach { service =>
                  queue.offer(ServiceEvent.ServiceDown(service.id))
                }

              filteredServices
                .filter(service => !services.contains(service))

            }
            .pipe(notifyServiceEvent(_, settingsLabels, namespace, queue))

          apply(settingsLabels,
                queue,
                servicesByNamespaces + (namespace -> filteredServices))
      }
    }

  private def notifyServiceEvent(
      services: List[KubernetesService],
      settingsLabels: Labels,
      namespace: String,
      queue: SourceQueueWithComplete[ServiceEvent])(implicit
      system: ActorSystem[_],
      executionContext: ExecutionContext,
      reflectionClient: ReflectionClient) =
    services
      .flatMap(createEndpoint(settingsLabels, _))
      .foreach {
        case ServiceFoundWithFile(id,
                                  serviceName,
                                  openApiService,
                                  grpcServer) =>
          for {
            _ <- openApiService.fold(Future.unit) {
              case OpenApiService(address, specificationPath, useProxy) =>
                downloadFile(
                  queue,
                  Service
                    .OpenApi(id,
                             serviceName,
                             address,
                             Map(Metadata.Provider -> "container",
                                 Metadata.File     -> specificationPath,
                                 Namespace         -> namespace),
                             useProxy = useProxy)
                )
            }
            _ <- loadSchema(queue, id, serviceName, namespace, grpcServer)
          } yield ()
      }

  private def createEndpoint(
      settingsLabels: Labels,
      service: KubernetesService): List[ServiceFoundWithFile] =
    settingsLabels
      .extractLabels(service.metadata.labels)
      .map { case Labels(_, maybeOpenApi, maybeGrpc) =>
        val openApiService = maybeOpenApi.map {
          case OpenApiLabels(port, protocol, specificationPath, useProxy) =>
            val address =
              s"$protocol://${service.copySpec.clusterIP}:${port.toInt}$specificationPath"
            val useProxyValue =
              allCatch.opt(useProxy.toBoolean).getOrElse(false)
            OpenApiService(address, specificationPath, useProxyValue)
        }
        val grpc = maybeGrpc.map { case GrpcLabels(port, tls) =>
          Service.Grpc.Server(service.copySpec.clusterIP,
                              port.toInt,
                              allCatch.opt(tls.toBoolean).getOrElse(false))
        }
        ServiceFoundWithFile(service.uid, service.name, openApiService, grpc)
      }
      .toList

  private def downloadFile(queue: SourceQueueWithComplete[ServiceEvent],
                           openapi: Service.OpenApi)(implicit
      system: ActorSystem[_],
      executionContext: ExecutionContext): Future[Unit] =
    Http()
      .singleRequest(HttpRequest(uri = openapi.file))
      .flatMap { response =>
        Unmarshaller.stringUnmarshaller(response.entity)
      }
      .flatMap { content =>
        queue
          .offer(ServiceEvent.ServiceUp(openapi.copy(file = content)))
          .map(_ => ())
      }
      .recover { throwable =>
        logger.warn("There was an error while download the file {}", throwable)
      }
  private def loadSchema(queue: SourceQueueWithComplete[ServiceEvent],
                         id: String,
                         serviceName: String,
                         namespace: String,
                         grpcServer: Option[Service.Grpc.Server])(implicit
      reflectionClient: ReflectionClient,
      executionContext: ExecutionContext): Future[Unit] =
    grpcServer.fold(Future.unit) { server =>
      val address = s"${server.address}:${server.port}"
      reflectionClient
        .loadSchema(server)
        .flatMap { schema =>
          val metadata = Map(
            Metadata.Provider -> "container",
            Metadata.File     -> address,
            Namespace         -> namespace
          )
          queue
            .offer(ServiceEvent.ServiceUp(Service
              .Grpc(id, serviceName, schema, Map(address -> server), metadata)))
            .map(_ => ())
        }
        .recover { throwable =>
          logger.warn("There was an error while retrieving the schema {}",
                      throwable)
        }
    }
}
