package restui.servicediscovery.kubernetes

import akka.actor.{Actor, ActorLogging}
import restui.servicediscovery.Models._
import restui.servicediscovery.ServiceDiscoveryProvider
import skuber.Service

class EndpointsActor(settingsLabels: Labels, callback: ServiceDiscoveryProvider.Callback) extends Actor with ActorLogging {
  import EndpointsActor._
  override def receive: Actor.Receive = handleMessage(Map.empty)
  private def handleMessage(servicesByNamespaces: Map[String, List[Service]]): Receive = {
    case (namespace: String, newServices: List[Service]) =>
      servicesByNamespaces.get(namespace) match {
        case None =>
          val filteredServices = newServices.filter(service => getLabels(service.metadata.labels).isDefined)
          filteredServices.flatMap(createEndpoint).foreach(endpoint => callback(Up(endpoint)))
          context.become(handleMessage(servicesByNamespaces + (namespace -> filteredServices)))
        case Some(services) =>
          val filteredServices = newServices.filter(service => getLabels(service.metadata.labels).isDefined)
          val removed          = services.filter(service => !filteredServices.contains(service))
          val added            = filteredServices.filter(service => !services.contains(service))
          removed.flatMap(createEndpoint).foreach(endpoint => callback(Down(endpoint)))
          added.flatMap(createEndpoint).foreach(endpoint => callback(Up(endpoint)))
          context.become(handleMessage(servicesByNamespaces + (namespace -> filteredServices)))
      }
      sender() ! Ack
    case Init     => sender() ! Ack
    case Complete => sender() ! Ack
  }
  private def getLabels(labels: Map[String, String]): Option[Labels] =
    for {
      port <- labels.get(settingsLabels.port)
      swaggerPath = labels.get(settingsLabels.swaggerPath).getOrElse("/swagger.yaml")
      protocol    = labels.get(settingsLabels.protocol).getOrElse("http")
    } yield Labels(protocol, port, swaggerPath)

  private def createEndpoint(service: Service): Option[Endpoint] = {
    val serviceName = service.name
    getLabels(service.metadata.labels).map {
      case Labels(protocol, port, swaggerPath) =>
        val address = s"$protocol://${service.copySpec.clusterIP}:$port$swaggerPath"
        Endpoint(serviceName, address)
    }
  }
}

object EndpointsActor {
  case object Init
  case object Complete
  case object Ack
}
