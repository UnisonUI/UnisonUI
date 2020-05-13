package restui.servicediscovery

import Models.Event
import com.typesafe.config.Config
import scala.util.Try

trait ServiceDiscoveryProvider {
  val name: String
  def initialise(config: Config, callback: ServiceDiscoveryProvider.Callback): Try[Unit]
}

object ServiceDiscoveryProvider {
  type Callback = Event => Unit
}
