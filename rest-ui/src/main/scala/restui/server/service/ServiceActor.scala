package restui.server.service

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import akka.stream.scaladsl.SourceQueueWithComplete
import restui.models._
import restui.specifications.Validator

object ServiceActor {
  sealed trait Message
  case class Add(from: ActorRef[Unit], provider: String, service: ServiceEvent) extends Message
  case class Get(from: ActorRef[Option[Service]], serviceId: String)            extends Message
  case class GetAll(from: ActorRef[List[Service]])                              extends Message

  def apply(queue: SourceQueueWithComplete[Event], services: Map[String, Service] = Map.empty): Behavior[Message] =
    Behaviors.receive { (context, message) =>
      message match {
        case Add(sender, provider, ServiceEvent.ServiceUp(service: Service.OpenApi)) if !Validator.isValid(service.file) =>
          context.log.debug(s"Invalid specification from $provider")
          sender ! ()
          Behaviors.same
        case Add(sender, provider, ServiceEvent.ServiceUp(service)) =>
          context.log.debug("{} got a new service", provider)
          val serviceWithHash    = computeSha1(service)
          val serviceNameChanged = hasServiceNameChanged(services, serviceWithHash)

          if (serviceNameChanged)
            queue.offer(Event.ServiceDown(serviceWithHash.id))
          if (isNewService(services, serviceWithHash) || serviceNameChanged)
            queue.offer(Event.ServiceUp(serviceWithHash.toEvent))

          if (hasContentChanged(services, serviceWithHash))
            queue.offer(Event.ServiceContentChanged(serviceWithHash.id))
          sender ! ()
          apply(queue, services + (serviceWithHash.id -> serviceWithHash))
        case Add(sender, provider, ServiceEvent.ServiceDown(serviceId)) =>
          queue.offer(Event.ServiceDown(serviceId))
          context.log.debug("{} removed a service", provider)
          sender ! ()
          apply(queue, services - serviceId)
        case Get(sender, serviceId) =>
          sender ! services.get(serviceId)
          Behaviors.same
        case GetAll(sender) =>
          sender ! services.values.toList
          Behaviors.same
      }
    }

  private def hasServiceNameChanged(services: Map[String, Service], service: Service): Boolean =
    service match {
      case openapiService: Service.OpenApi =>
        services.exists {
          case (id, currentService: Service.OpenApi) =>
            id == service.id && currentService.name != openapiService.name
          case _ => false
        }
      case _ => false
    }

  private def computeSha1(service: Service): Service =
    service match {
      case openapi: Service.OpenApi =>
        val md       = java.security.MessageDigest.getInstance("SHA-1")
        val sha1Hash = md.digest(openapi.file.getBytes("UTF-8")).map("%02x".format(_)).mkString
        openapi.copy(hash = sha1Hash)
      case _ => service
    }

  private def isNewService(services: Map[String, Service], service: Service): Boolean = !services.contains(service.id)

  private def hasContentChanged(services: Map[String, Service], service: Service): Boolean =
    service match {
      case serviceOpenapi: Service.OpenApi =>
        services.exists {
          case (id, Service.OpenApi(_, _, _, _, _, hash)) => id == service.id && hash != serviceOpenapi.hash
          case _                                          => false
        }
      case _ => false
    }
}
