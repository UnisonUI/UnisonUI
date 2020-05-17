package restui.servicediscovery.docker

import scala.util.Try

import com.typesafe.config.Config
import org.slf4j.LoggerFactory
import restui.servicediscovery.ServiceDiscoveryProvider

class DockerProvider extends ServiceDiscoveryProvider {

  private val logger = LoggerFactory.getLogger(classOf[DockerProvider])

  override def initialise(config: Config, callback: ServiceDiscoveryProvider.Callback): Try[Unit] =
    Try {
      val settings = Settings.from(config)
      logger.debug("Initialising docker provider with {}", settings)
      new DockerClient(settings, callback).listCurrentAndFutureEndpoints
    }

}
