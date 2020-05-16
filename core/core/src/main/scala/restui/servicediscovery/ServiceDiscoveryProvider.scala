package restui.servicediscovery

import scala.util.Try

import com.typesafe.config.Config
import restui.servicediscovery.Models.Event

trait ServiceDiscoveryProvider {
  val name: String
  def initialise(config: Config, callback: ServiceDiscoveryProvider.Callback): Try[Unit]
}

object ServiceDiscoveryProvider {
  type Callback = Event => Unit
}
