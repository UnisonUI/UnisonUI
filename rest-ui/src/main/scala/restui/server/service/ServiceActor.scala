package restui.server.service

import akka.actor.{Actor, ActorLogging, Props}
import akka.stream.scaladsl.SourceQueueWithComplete
import restui.server.http.Models
import restui.servicediscovery.models._

class ServiceActor(queue: SourceQueueWithComplete[Models.Event]) extends Actor with ActorLogging {
  import ServiceActor._
  override def receive: Receive = handleReceive(Map.empty)

  private def handleReceive(services: Map[String, Service]): Receive = {
    case (provider: String, ServiceUp(service)) =>
      log.debug("{} got a new service", provider)
      queue.offer(Models.ServiceUp(service.serviceName))
      context.become(handleReceive(services + (service.serviceName -> service)))

    case (provider: String, ServiceDown(serviceName)) =>
      queue.offer(Models.ServiceDown(serviceName))
      log.debug("{} removed a service", provider)
      context.become(handleReceive(services - serviceName))

    case Get(serviceName) => sender() ! services.get(serviceName)
    case GetAll           => sender() ! services.values.toList
  }

}

object ServiceActor {
  def props(queue: SourceQueueWithComplete[Models.Event]): Props = Props(classOf[ServiceActor], queue)
  case class Get(serviceName: String)
  case object GetAll
}
