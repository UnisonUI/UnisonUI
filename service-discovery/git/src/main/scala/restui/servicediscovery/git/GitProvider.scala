package restui.servicediscovery.git

import scala.util.Try

import akka.actor.ActorSystem
import com.typesafe.config.Config
import org.slf4j.LoggerFactory
import restui.servicediscovery.ServiceDiscoveryProvider
import restui.servicediscovery.git.settings.Settings

class GitProvider extends ServiceDiscoveryProvider {

  private val logger = LoggerFactory.getLogger(classOf[GitProvider])

  override def start(actorSystem: ActorSystem, config: Config, callback: ServiceDiscoveryProvider.Callback): Try[Unit] =
    Try {
      implicit val system: ActorSystem = actorSystem
      val settings                     = Settings.from(config)

      logger.debug("Initialising git provider with {}", settings)
    }

}
