package restui.servicediscovery.kubernetes

import scala.util.Try

import com.typesafe.config.Config
import org.slf4j.LoggerFactory
import restui.servicediscovery.ServiceDiscoveryProvider

class KubernetesProvider extends ServiceDiscoveryProvider {

  private val logger = LoggerFactory.getLogger(classOf[KubernetesProvider])

  override def initialise(config: Config, callback: ServiceDiscoveryProvider.Callback): Try[Unit] =
    Try {
      val settings = Settings.from(config)
      logger.debug("Initialising Kubernetes provider with {}", settings)
      new KubernetesClient(settings, callback).listCurrentAndFutureEndpoints
    }

}
