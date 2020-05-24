package restui.servicediscovery.kubernetes

import scala.util.Try

import akka.actor.ActorSystem
import com.typesafe.config.Config
import com.typesafe.scalalogging.LazyLogging
import restui.servicediscovery.ServiceDiscoveryProvider

class KubernetesProvider extends ServiceDiscoveryProvider with LazyLogging {

  override def start(actorSystem: ActorSystem, config: Config, callback: ServiceDiscoveryProvider.Callback): Try[Unit] =
    Try {
      implicit val system: ActorSystem = actorSystem
      val settings                     = Settings.from(config)
      logger.debug("Initialising Kubernetes provider")
      new KubernetesClient(settings, callback).listCurrentAndFutureServices
    }

}
