package restui.providers.kubernetes

import akka.actor.{Actor, ActorLogging, ActorSystem}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.unmarshalling.Unmarshaller
import akka.stream.scaladsl.SourceQueueWithComplete
import restui.models._
import skuber.{Service => KubernetesService}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class ServiceActor(settingsLabels: Labels,
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
            case (id, serviceName, specificationPath, file, useProxy) =>
              val metadata = Map(Metadata.Provider -> "kubernetes",
                                 Metadata.File -> specificationPath,
                                 Namespace     -> namespace)
              downloadFile(Service
                .OpenApi(id, serviceName, file, metadata, useProxy = useProxy))
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

          removed.flatMap(createEndpoint).foreach { case (id, _, _, _, _) =>
            queue.offer(ServiceEvent.ServiceDown(id))
          }
          added.flatMap(createEndpoint).foreach {
            case (id, serviceName, specificationPath, file, useProxy) =>
              val metadata = Map(Metadata.Provider -> "kubernetes",
                                 Metadata.File -> specificationPath,
                                 Namespace     -> namespace)
              downloadFile(Service
                .OpenApi(id, serviceName, file, metadata, useProxy = useProxy))
          }

          context.become(
            handleMessage(
              servicesByNamespaces + (namespace -> filteredServices)))
      }
      sender() ! Ack
    case Init     => sender() ! Ack
    case Complete => sender() ! Ack
  }

  private def downloadFile(service: Service): Future[Unit] =
    service match {
      case openapi: Service.OpenApi =>
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
            log.warning("There was an error while download the file {}",
                        throwable)
          }
      case _ =>
        log.warning("Not supported yet")
        Future.successful(())
    }
  private def createEndpoint(
      service: KubernetesService): Option[ServiceFoundWithFile] =
    getLabels(service.metadata.labels).map {
      case Labels(protocol, port, specificationPath, useProxy) =>
        val address =
          s"$protocol://${service.copySpec.clusterIP}:$port$specificationPath"
        val useProxyValue = Try(useProxy.toBoolean).getOrElse(false)
        (service.uid, service.name, specificationPath, address, useProxyValue)
    }

  private def getLabels(labels: Map[String, String]): Option[Labels] =
    for {
      port <- labels.get(settingsLabels.port)
      specificationPath =
        labels
          .get(settingsLabels.specificationPath)
          .getOrElse("/specification.yaml")
      protocol = labels.get(settingsLabels.protocol).getOrElse("http")
      useProxy = labels.get(settingsLabels.useProxy).getOrElse("false")
    } yield Labels(protocol, port, specificationPath, useProxy)

}

object ServiceActor {
  private type ServiceFoundWithFile = (String, String, String, String, Boolean)
  private val Namespace = "namespace"
  case object Init
  case object Complete
  case object Ack
}
