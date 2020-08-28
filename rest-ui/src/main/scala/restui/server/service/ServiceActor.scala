package restui.server.service

import akka.actor.{Actor, ActorLogging, Props}
import akka.stream.scaladsl.SourceQueueWithComplete
import io.circe.Json
import io.circe.yaml.syntax._
import restui.models._
import restui.server.Base64
import restui.specifications.{parse, Validator}

class ServiceActor(queue: SourceQueueWithComplete[Event]) extends Actor with ActorLogging {
  import ServiceActor._
  override def receive: Receive = handleReceive(Map.empty)

  private def handleReceive(services: Map[String, Service]): Receive = {
    case (provider: String, ServiceEvent.ServiceUp(service)) if !Validator.isValid(service.file) =>
      log.debug(s"Invalid specification from $provider")
      sender() ! Ack
    case (provider: String, ServiceEvent.ServiceUp(service)) =>
      log.debug("{} got a new service", provider)

      val serviceNameChanged = hasServiceNameChanged(services, service)

      if (serviceNameChanged)
        queue.offer(Event.ServiceDown(service.id))

      if (isNewService(services, service) || serviceNameChanged)
        queue.offer(Event.ServiceUp(service.id, service.name, service.metadata))
      val updatedService = rewriteForProxy(service)
      context.become(handleReceive(services + (service.id -> updatedService)))
      sender() ! Ack

    case (provider: String, ServiceEvent.ServiceDown(serviceId)) =>
      queue.offer(Event.ServiceDown(serviceId))
      log.debug("{} removed a service", provider)
      context.become(handleReceive(services - serviceId))
      sender() ! Ack

    case Get(serviceId) => sender() ! services.get(serviceId)
    case GetAll         => sender() ! services.values.toList
    case Init           => sender() ! Ack
    case Complete       => sender() ! Ack
  }

  private def hasServiceNameChanged(services: Map[String, Service], service: Service): Boolean =
    services.exists {
      case (id, currentService) =>
        id == service.id && currentService.name != service.name
    }

  private def isNewService(services: Map[String, Service], service: Service): Boolean = !services.contains(service.id)

  private def rewriteForProxy(service: Service): Service = {
    val json = parse(service.file).getOrElse(Json.obj())
    val newJson =
      json.hcursor
        .downField("servers")
        .withFocus(
          _.mapArray(
            _.map(
              _.hcursor
                .downField("url")
                .withFocus(_.mapString { url =>
                  val urlEncoded = Base64.encode(url)
                  s"/proxy/$urlEncoded"
                })
                .top
                .get)))
        .top
        .getOrElse(json)
    service.copy(file = newJson.asYaml.spaces2)
  }

}

object ServiceActor {
  def props(queue: SourceQueueWithComplete[Event]): Props = Props(classOf[ServiceActor], queue)
  case class Get(serviceId: String)
  case object GetAll
  case object Init
  case object Complete
  case object Ack
}
