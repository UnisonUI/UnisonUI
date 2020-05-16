package restui.server.service

import akka.actor.{Actor, ActorLogging, Props}
import akka.stream.scaladsl.SourceQueueWithComplete
import restui.server.http.{Models => HttpModels}
import restui.servicediscovery.Models._

class EndpointsActor(queue: SourceQueueWithComplete[HttpModels.Event]) extends Actor with ActorLogging {
  import EndpointsActor._
  override def receive: Receive = handleReceive(Map.empty)

  private def handleReceive(endpoints: Map[String, (String, Endpoint)]): Receive = {
    case (provider: String, Up(endpoint)) =>
      log.info("{} got a new endpoint", provider)
      queue.offer(HttpModels.Up(endpoint.serviceName, provider))
      context.become(handleReceive(endpoints + (endpoint.serviceName -> (provider -> endpoint))))
    case (provider: String, Down(endpoint)) =>
      queue.offer(HttpModels.Down(endpoint.serviceName, provider))
      log.info("{} removed an endpoint", provider)
      context.become(handleReceive(endpoints - endpoint.serviceName))
    case Get(serviceName) => sender() ! endpoints.get(serviceName).map(_._2)
    case GetAll           => sender() ! endpoints.values.toList
    case m                => log.warning("Unmatch {}", m)
  }

}

object EndpointsActor {
  def props(queue: SourceQueueWithComplete[HttpModels.Event]): Props = Props(classOf[EndpointsActor], queue)
  case class Get(serviceName: String)
  case object GetAll
}
