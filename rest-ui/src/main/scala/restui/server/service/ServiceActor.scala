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
      queue.offer(Event.ServiceUp(service.serviceName))
      context.become(handleReceive(services + (service.serviceName -> service)))

    case (provider: String, Event.ServiceDown(serviceName)) =>
      queue.offer(Event.ServiceDown(serviceName))
      log.debug("{} removed a service", provider)
      context.become(handleReceive(services - serviceName))

    case Get(serviceName) => sender() ! services.get(serviceName)
    case GetAll           => sender() ! services.values.toList
  }

}

object ServiceActor {
  def props(queue: SourceQueueWithComplete[Event]): Props = Props(classOf[ServiceActor], queue)
  case class Get(serviceName: String)
  case object GetAll
}
