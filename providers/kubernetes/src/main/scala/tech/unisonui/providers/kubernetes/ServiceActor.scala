package tech.unisonui.providers.kubernetes

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

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try
import scala.util.chaining._

object ServiceActor extends LazyLogging {
  private final case class ServiceFoundWithFile(
      id: String,
      service: String,
      specificationPath: String,
      address: Option[String],
      useProxyValue: Boolean,
      grpcServer: Option[Service.Grpc.Server])
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
            getLabels(settingsLabels, service.metadata.labels).isDefined)
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
                                  specificationPath,
                                  maybeFile,
                                  useProxy,
                                  grpcServer) =>
          for {
            _ <- maybeFile.fold(Future.unit) { file =>
              downloadFile(
                queue,
                Service
                  .OpenApi(id,
                           serviceName,
                           file,
                           Map(Metadata.Provider -> "kubernetes",
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
    getLabels(settingsLabels, service.metadata.labels).map {
      case Labels(protocol,
                  port,
                  specificationPath,
                  useProxy,
                  grpcPort,
                  grpcTls) =>
        val address = Try(port.toInt).toOption.flatMap {
          case 0    => None
          case port => Some(port)
        }.map(port =>
          s"$protocol://${service.copySpec.clusterIP}:$port$specificationPath")
        val useProxyValue = Try(useProxy.toBoolean).getOrElse(false)
        val server = grpcPort
          .flatMap(port => Try(port.toInt).toOption)
          .map(port =>
            Service.Grpc.Server(service.copySpec.clusterIP,
                                port,
                                Try(grpcTls.toBoolean).getOrElse(false)))
        ServiceFoundWithFile(service.uid,
                             service.name,
                             specificationPath,
                             address,
                             useProxyValue,
                             server)
    }.toList

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
            Metadata.Provider -> "kubernetes",
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

  private def getLabels(settingsLabels: Labels,
                        labels: Map[String, String]): Option[Labels] =
    for {
      port <- labels.get(settingsLabels.port)
      specificationPath =
        labels.getOrElse(settingsLabels.specificationPath,
                         "/specification.yaml")
      protocol = labels.getOrElse(settingsLabels.protocol, "http")
      useProxy = labels.getOrElse(settingsLabels.useProxy, "false")
      grpcPort = settingsLabels.grpcPort.flatMap(labels.get(_))
      grpcTls  = labels.getOrElse(settingsLabels.grpcTls, "false")
    } yield
      Labels(protocol, port, specificationPath, useProxy, grpcPort, grpcTls)
}
