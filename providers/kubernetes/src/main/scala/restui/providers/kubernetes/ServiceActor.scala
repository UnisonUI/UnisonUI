package restui.providers.kubernetes

import akka.actor.{Actor, ActorLogging, ActorSystem}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, Uri}
import akka.http.scaladsl.unmarshalling.Unmarshaller
import akka.stream.scaladsl.SourceQueueWithComplete
import restui.grpc.ReflectionClient
import restui.models._
import skuber.{Service => KubernetesService}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class ServiceActor(settingsLabels: Labels,
                   reflectionClient: ReflectionClient,
                   queue: SourceQueueWithComplete[ServiceEvent])
    extends Actor
    with ActorLogging {
  import ServiceActor._
  private implicit val system: ActorSystem                = context.system
  private implicit val executionContext: ExecutionContext = context.dispatcher

  override def receive: Actor.Receive = handleMessage(Map.empty)

  private def handleMessage(
      servicesByNamespaces: Map[String, List[KubernetesService]]): Receive = {
    case (namespace: String, newServices: List[KubernetesService]) =>
      servicesByNamespaces.get(namespace) match {
        case None =>
          val filteredServices = newServices.filter(service =>
            getLabels(service.metadata.labels).isDefined)
          filteredServices.flatMap(createEndpoint).foreach {
            case (id,
                  serviceName,
                  specificationPath,
                  file,
                  useProxy,
                  grpcEndpoint) =>
              for {
                _ <- downloadFile(
                  Service
                    .OpenApi(id,
                             serviceName,
                             file,
                             Map(Metadata.Provider -> "kubernetes",
                                 Metadata.File     -> specificationPath,
                                 Namespace         -> namespace),
                             useProxy = useProxy))
                _ <- loadSchema(id, serviceName, namespace, grpcEndpoint)
              } yield ()
          }
          context.become(
            handleMessage(
              servicesByNamespaces + (namespace -> filteredServices)))
        case Some(services) =>
          val filteredServices = newServices.filter(service =>
            getLabels(service.metadata.labels).isDefined)
          val removed =
            services.filter(service => !filteredServices.contains(service))
          val added =
            filteredServices.filter(service => !services.contains(service))

          removed.flatMap(createEndpoint).foreach { case (id, _, _, _, _, _) =>
            queue.offer(ServiceEvent.ServiceDown(id))
          }
          added.flatMap(createEndpoint).foreach {
            case (id,
                  serviceName,
                  specificationPath,
                  file,
                  useProxy,
                  grpcEndpoint) =>
              for {
                _ <- downloadFile(
                  Service
                    .OpenApi(id,
                             serviceName,
                             file,
                             Map(Metadata.Provider -> "kubernetes",
                                 Metadata.File     -> specificationPath,
                                 Namespace         -> namespace),
                             useProxy = useProxy))
                _ <- loadSchema(id, serviceName, namespace, grpcEndpoint)
              } yield ()
          }

          context.become(
            handleMessage(
              servicesByNamespaces + (namespace -> filteredServices)))
      }
      sender() ! Ack
    case Init     => sender() ! Ack
    case Complete => sender() ! Ack
  }

  private def createEndpoint(
      service: KubernetesService): Option[ServiceFoundWithFile] =
    getLabels(service.metadata.labels).map {
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
    }

  private def downloadFile(openapi: Service.OpenApi): Future[Unit] =
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
        log.warning("There was an error while download the file {}", throwable)
      }
  private def loadSchema(id: String,
                         serviceName: String,
                         namespace: String,
                         grpcEndpoint: Option[String]): Future[Unit] =
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
          log.warning("There was an error while retrieving the schema {}",
                      throwable)
        }
    }

  private def getLabels(labels: Map[String, String]): Option[Labels] =
    for {
      port <- labels.get(settingsLabels.port)
      specificationPath =
        labels.getOrElse(settingsLabels.specificationPath,
                         "/specification.yaml")
      protocol     = labels.getOrElse(settingsLabels.protocol, "http")
      useProxy     = labels.getOrElse(settingsLabels.useProxy, "false")
      grpcEndpoint = settingsLabels.grpcEndpoint.flatMap(labels.get)
    } yield Labels(protocol, port, specificationPath, useProxy, grpcEndpoint)

}

object ServiceActor {
  private type ServiceFoundWithFile =
    (String, String, String, String, Boolean, Option[String])
  private val Namespace = "namespace"
  case object Init
  case object Complete
  case object Ack
}
