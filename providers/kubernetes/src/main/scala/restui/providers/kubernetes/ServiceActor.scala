package restui.providers.kubernetes

import scala.concurrent.{ExecutionContext, Future}

import akka.actor.{Actor, ActorLogging, ActorSystem}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.unmarshalling.Unmarshaller
import restui.models._
import restui.providers.Provider
import skuber.{Service => KubernetesService}

class ServiceActor(settingsLabels: Labels, callback: Provider.Callback) extends Actor with ActorLogging {
  import ServiceActor._
  private implicit val system: ActorSystem                = context.system
  private implicit val executionContext: ExecutionContext = context.dispatcher

  override def receive: Actor.Receive = handleMessage(Map.empty)

  private def handleMessage(servicesByNamespaces: Map[String, List[KubernetesService]]): Receive = {
    case (namespace: String, newServices: List[KubernetesService]) =>
      val metadata = Map(Namespace -> namespace)
      servicesByNamespaces.get(namespace) match {
        case None =>
          val filteredServices = newServices.filter(service => getLabels(service.metadata.labels).isDefined)
          filteredServices.flatMap(createEndpoint).foreach {
            case (serviceName, file) =>
              downloadFile(Service(serviceName, file, metadata))
          }
          context.become(handleMessage(servicesByNamespaces + (namespace -> filteredServices)))
        case Some(services) =>
          val filteredServices = newServices.filter(service => getLabels(service.metadata.labels).isDefined)
          val removed          = services.filter(service => !filteredServices.contains(service))
          val added            = filteredServices.filter(service => !services.contains(service))

          removed.flatMap(createEndpoint).foreach { case (serviceName, _) => callback(ServiceEvent.ServiceDown(serviceName)) }
          added.flatMap(createEndpoint).foreach {
            case (serviceName, file) =>
              downloadFile(Service(serviceName, file, metadata))
          }

          context.become(handleMessage(servicesByNamespaces + (namespace -> filteredServices)))
      }
      sender() ! Ack
    case Init     => sender() ! Ack
    case Complete => sender() ! Ack
  }

  private def downloadFile(service: Service): Future[Unit] =
    Http()
      .singleRequest(HttpRequest(uri = service.file.content))
      .flatMap { response =>
        Unmarshaller.stringUnmarshaller(response.entity)
      }
      .map { content =>
        callback(ServiceEvent.ServiceUp(service.copy(file = service.file.copy(content = content))))
      }
      .recover { throwable =>
        log.warning("There was an error while download the file {}", throwable)
      }

  private def createEndpoint(service: KubernetesService): Option[(String, OpenApiFile)] =
    getLabels(service.metadata.labels).map {
      case Labels(protocol, port, swaggerPath) =>
        val address = s"$protocol://${service.copySpec.clusterIP}:$port$swaggerPath"
        (service.name, OpenApiFile(ContentType.fromString(address), address))
    }

  private def getLabels(labels: Map[String, String]): Option[Labels] =
    for {
      port <- labels.get(settingsLabels.port)
      swaggerPath = labels.get(settingsLabels.swaggerPath).getOrElse("/swagger.yaml")
      protocol    = labels.get(settingsLabels.protocol).getOrElse("http")
    } yield Labels(protocol, port, swaggerPath)

}

object ServiceActor {
  private val Namespace = "namespace"
  case object Init
  case object Complete
  case object Ack
}
