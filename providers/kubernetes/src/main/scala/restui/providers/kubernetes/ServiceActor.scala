package restui.providers.kubernetes

import scala.concurrent.{ExecutionContext, Future}

import akka.actor.{Actor, ActorLogging, ActorSystem}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.unmarshalling.Unmarshaller
import akka.stream.scaladsl.SourceQueueWithComplete
import restui.models._
import skuber.{Service => KubernetesService}

class ServiceActor(settingsLabels: Labels, queue: SourceQueueWithComplete[ServiceEvent]) extends Actor with ActorLogging {
  import ServiceActor._
  private implicit val system: ActorSystem                = context.system
  private implicit val executionContext: ExecutionContext = context.dispatcher

  override def receive: Actor.Receive = handleMessage(Map.empty)

  private def handleMessage(servicesByNamespaces: Map[String, List[KubernetesService]]): Receive = {
    case (namespace: String, newServices: List[KubernetesService]) =>
      servicesByNamespaces.get(namespace) match {
        case None =>
          val filteredServices = newServices.filter(service => getLabels(service.metadata.labels).isDefined)
          filteredServices.flatMap(createEndpoint).foreach {
            case (id, serviceName, specificationPath, file) =>
              val metadata = Map(Metadata.Provider -> "kubernetes", Metadata.File -> specificationPath, Namespace -> namespace)
              downloadFile(Service(id, serviceName, file, metadata))
          }
          context.become(handleMessage(servicesByNamespaces + (namespace -> filteredServices)))
        case Some(services) =>
          val filteredServices = newServices.filter(service => getLabels(service.metadata.labels).isDefined)
          val removed          = services.filter(service => !filteredServices.contains(service))
          val added            = filteredServices.filter(service => !services.contains(service))

          removed.flatMap(createEndpoint).foreach { case (id, _, _, _) => queue.offer(ServiceEvent.ServiceDown(id)) }
          added.flatMap(createEndpoint).foreach {
            case (id, serviceName, specificationPath, file) =>
              val metadata = Map(Metadata.Provider -> "kubernetes", Metadata.File -> specificationPath, Namespace -> namespace)
              downloadFile(Service(id, serviceName, file, metadata))
          }

          context.become(handleMessage(servicesByNamespaces + (namespace -> filteredServices)))
      }
      sender() ! Ack
    case Init     => sender() ! Ack
    case Complete => sender() ! Ack
  }

  private def downloadFile(service: Service): Future[Unit] =
    Http()
      .singleRequest(HttpRequest(uri = service.file))
      .flatMap { response =>
        Unmarshaller.stringUnmarshaller(response.entity)
      }
      .flatMap { content =>
        queue.offer(ServiceEvent.ServiceUp(service.copy(file = content))).map(_ => ())
      }
      .recover { throwable =>
        log.warning("There was an error while download the file {}", throwable)
      }

  private def createEndpoint(service: KubernetesService): Option[ServiceFoundWithFile] =
    getLabels(service.metadata.labels).map {
      case Labels(protocol, port, specificationPath) =>
        val address = s"$protocol://${service.copySpec.clusterIP}:$port$specificationPath"
        (service.uid, service.name, specificationPath, address)
    }

  private def getLabels(labels: Map[String, String]): Option[Labels] =
    for {
      port <- labels.get(settingsLabels.port)
      specificationPath = labels.get(settingsLabels.specificationPath).getOrElse("/specification.yaml")
      protocol          = labels.get(settingsLabels.protocol).getOrElse("http")
    } yield Labels(protocol, port, specificationPath)

}

object ServiceActor {
  private type ServiceFoundWithFile = (String, String, String, String)
  private val Namespace = "namespace"
  case object Init
  case object Complete
  case object Ack
}
