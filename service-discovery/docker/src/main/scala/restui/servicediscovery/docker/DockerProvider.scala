package restui.servicediscovery.docker

import restui.servicediscovery.ServiceDiscoveryProvider
import com.typesafe.config.Config
import org.slf4j.LoggerFactory

import scala.util.Try

class DockerProvider extends ServiceDiscoveryProvider {

  private val logger = LoggerFactory.getLogger(classOf[DockerProvider])

  override val name: String = "docker"

  override def initialise(config: Config, callback: ServiceDiscoveryProvider.Callback): Try[Unit] =
    Try {
      val settings = Settings.from(config)
      logger.debug("Initialising docker provider with {}", settings)
      new DockerClient(settings, callback).listCurrentAndFutureEndpoints
    }

}
