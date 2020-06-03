package restui.server.service

import akka.actor.{Actor, ActorLogging, Props}
import akka.stream.scaladsl.SourceQueueWithComplete
import restui.models._

class ServiceActor(queue: SourceQueueWithComplete[Event]) extends Actor with ActorLogging {
  import ServiceActor._
  override def receive: Receive = handleReceive(Map.empty)

  private def handleReceive(services: Map[String, Service]): Receive = {
    case (provider: String, ServiceEvent.ServiceUp(service)) =>
      log.debug("{} got a new service", provider)

      val hasServiceNameChanged = hasServiceNameChanged(services, service)

      if (hasServiceNameChanged)
        queue.offer(Event.ServiceDown(service.id))

      if (isNewService(services, service) || hasServiceNameChanged)
        queue.offer(Event.ServiceUp(service.id, service.name, service.metadata))

      context.become(handleReceive(services + (service.id -> service)))

    case (provider: String, Event.ServiceDown(serviceId)) =>
      queue.offer(Event.ServiceDown(serviceId))
      log.debug("{} removed a service", provider)
      context.become(handleReceive(services - serviceId))

    case Get(serviceId) => sender() ! services.get(serviceId)
    case GetAll         => sender() ! services.values.toList
  }

  private def hasServiceNameChanged(services: Map[String, Service], service: Service): Boolean =
    services.exists {
      case (id, currentService) =>
        id == service.id && currentService.name != service.name
    }

  private def isNewService(services: Map[String, Service], service: Service): Boolean = !services.contains(service.id)
}

object ServiceActor {
  def props(queue: SourceQueueWithComplete[Event]): Props = Props(classOf[ServiceActor], queue)
  case class Get(serviceId: String)
  case object GetAll
}
