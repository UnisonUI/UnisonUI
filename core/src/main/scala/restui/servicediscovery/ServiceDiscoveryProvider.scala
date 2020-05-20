package restui.servicediscovery

import scala.util.Try

import com.typesafe.config.Config
import restui.servicediscovery.models.ServiceEvent
import akka.actor.ActorSystem

trait ServiceDiscoveryProvider {
  def start(actorSystem: ActorSystem, config: Config, callback: ServiceDiscoveryProvider.Callback): Try[Unit]
}

object ServiceDiscoveryProvider {
  type Callback = ServiceEvent => Unit
}
