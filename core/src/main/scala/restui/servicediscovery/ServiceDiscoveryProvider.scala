package restui.servicediscovery

import scala.util.Try

import akka.actor.ActorSystem
import com.typesafe.config.Config
import restui.servicediscovery.models.ServiceEvent

trait ServiceDiscoveryProvider {
  def start(actorSystem: ActorSystem, config: Config, callback: ServiceDiscoveryProvider.Callback): Try[Unit]
}

object ServiceDiscoveryProvider {
  type Callback = ServiceEvent => Unit
}
