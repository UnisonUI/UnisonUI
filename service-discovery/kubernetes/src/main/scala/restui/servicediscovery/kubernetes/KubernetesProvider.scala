package restui.servicediscovery.kubernetes

import scala.util.Try

import com.typesafe.config.Config
import org.slf4j.LoggerFactory
import restui.servicediscovery.ServiceDiscoveryProvider
import akka.actor.ActorSystem

class KubernetesProvider extends ServiceDiscoveryProvider {

  private val logger = LoggerFactory.getLogger(classOf[KubernetesProvider])

  override def start(actorSystem: ActorSystem, config: Config, callback: ServiceDiscoveryProvider.Callback): Try[Unit] =
    Try {
      implicit val system: ActorSystem = actorSystem
      val settings                     = Settings.from(config)
      logger.debug("Initialising Kubernetes provider with {}", settings)
      new KubernetesClient(settings, callback).listCurrentAndFutureServices
    }

}
