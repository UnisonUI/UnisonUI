package restui.server.service

import akka.actor.{Actor, ActorLogging, Props}
import restui.servicediscovery.Models._

class EndpointsActor extends Actor with ActorLogging {
  import EndpointsActor._
  override def receive: Receive = handleReceive(List.empty)

  private def handleReceive(endpoints: List[Endpoint]): Receive = {
    case (provider, Up(endpoint)) =>
      log.info("{} got a new endpoint", provider)
      context.become(handleReceive(endpoints :+ endpoint))
    case (provider, Down(endpoint)) =>
      log.info("{} removed an endpoint", provider)
      context.become(handleReceive(endpoints.filterNot(_ == endpoint)))
    case Get => sender() ! endpoints
  }

}

object EndpointsActor {
  def props: Props = Props[EndpointsActor]
  case object Get
}
