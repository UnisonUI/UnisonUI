package restui.server.service

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import akka.stream.scaladsl.SourceQueueWithComplete
import restui.models._
import restui.specifications.Validator

object ServiceActor {
  sealed trait Message
  case class Add(provider: String, service: ServiceEvent) extends Message
  case class Get(from: ActorRef[Option[Service]], serviceId: String)
      extends Message
  case class GetAll(from: ActorRef[List[Service]]) extends Message

  def apply(queue: SourceQueueWithComplete[Event],
            services: Map[String, Service] = Map.empty): Behavior[Message] =
    Behaviors.receive { (context, message) =>
      message match {
        case Add(provider, ServiceEvent.ServiceUp(service: Service.OpenApi))
            if !Validator.isValid(service.file) =>
          context.log.debug(s"Invalid specification from $provider")
          Behaviors.same

        case Add(provider, ServiceEvent.ServiceUp(service)) =>
          context.log.debug("{} got a new service", provider)

          val serviceNameChanged = hasServiceNameChanged(services, service)

          if (serviceNameChanged) queue.offer(Event.ServiceDown(service.id))

          if (isNewService(services, service) || serviceNameChanged)
            queue.offer(Event.ServiceUp(service.toEvent))

          if (hasContentChanged(services, service))
            queue.offer(Event.ServiceContentChanged(service.id))

          apply(queue, services + (service.id -> service))

        case Add(provider, ServiceEvent.ServiceDown(serviceId)) =>
          queue.offer(Event.ServiceDown(serviceId))
          context.log.debug("{} removed a service", provider)
          apply(queue, services - serviceId)

        case Get(sender, serviceId) =>
          sender ! services.get(serviceId)
          Behaviors.same
        case GetAll(sender) =>
          sender ! services.values.toList
          Behaviors.same
      }
    }

  private def hasServiceNameChanged(services: Map[String, Service],
                                    service: Service): Boolean =
    service match {
      case openapiService: Service.OpenApi =>
        services.exists {
          case (id, currentService: Service.OpenApi) =>
            id == service.id && currentService.name != openapiService.name
          case _ => false
        }
      case _ => false
    }

  private def isNewService(services: Map[String, Service],
                           service: Service): Boolean =
    !services.contains(service.id)

  private def hasContentChanged(services: Map[String, Service],
                                service: Service): Boolean =
    services.exists { case (id, currentService) =>
      id == service.id && service.hash != currentService.hash
    }
}
