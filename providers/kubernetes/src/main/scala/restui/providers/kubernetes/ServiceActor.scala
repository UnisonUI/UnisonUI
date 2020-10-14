package restui.providers.kubernetes

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorSystem, Behavior}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, Uri}
import akka.http.scaladsl.unmarshalling.Unmarshaller
import akka.stream.scaladsl.SourceQueueWithComplete
import com.typesafe.scalalogging.LazyLogging
import restui.grpc.ReflectionClient
import restui.models._
import skuber.{Service => KubernetesService}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try
import scala.util.chaining._

object ServiceActor extends LazyLogging {
  private type ServiceFoundWithFile =
    (String, String, String, String, Boolean, Option[String])
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
                .foreach { case (id, _, _, _, _, _) =>
                  queue.offer(ServiceEvent.ServiceDown(id))
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
        case (id,
              serviceName,
              specificationPath,
              file,
              useProxy,
              grpcEndpoint) =>
          for {
            _ <- downloadFile(
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
            _ <- loadSchema(queue, id, serviceName, namespace, grpcEndpoint)
          } yield ()
      }

  private def createEndpoint(
      settingsLabels: Labels,
      service: KubernetesService): List[ServiceFoundWithFile] =
    getLabels(settingsLabels, service.metadata.labels).map {
      case Labels(protocol, port, specificationPath, useProxy, grpcEndpoint) =>
        val address =
          s"$protocol://${service.copySpec.clusterIP}:$port$specificationPath"
        val useProxyValue = Try(useProxy.toBoolean).getOrElse(false)
        (service.uid,
         service.name,
         specificationPath,
         address,
         useProxyValue,
         grpcEndpoint)
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
                         grpcEndpoint: Option[String])(implicit
      reflectionClient: ReflectionClient,
      executionContext: ExecutionContext): Future[Unit] =
    grpcEndpoint.fold(Future.unit) { endpoint =>
      val correctedEndoint =
        if (!endpoint.contains("//")) s"http://$endpoint" else endpoint
      val uri = Uri(correctedEndoint)
      val tls = uri.scheme == "https"
      val server =
        Service.Grpc.Server(uri.authority.host.toString,
                            uri.authority.port,
                            tls)

      reflectionClient
        .loadSchema(server)
        .flatMap { schema =>
          val metadata = Map(
            Metadata.Provider -> "kubernetes",
            Metadata.File     -> s"${uri.authority}",
            Namespace         -> namespace
          )
          queue
            .offer(
              ServiceEvent.ServiceUp(
                Service.Grpc(id,
                             serviceName,
                             schema,
                             Map(uri.authority.toString -> server),
                             metadata)))
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
      protocol     = labels.getOrElse(settingsLabels.protocol, "http")
      useProxy     = labels.getOrElse(settingsLabels.useProxy, "false")
      grpcEndpoint = settingsLabels.grpcEndpoint.flatMap(labels.get(_))
    } yield Labels(protocol, port, specificationPath, useProxy, grpcEndpoint)

}
