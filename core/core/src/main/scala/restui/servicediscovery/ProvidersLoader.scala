package restui.servicediscovery

import ServiceDiscoveryProvider.Callback
import com.typesafe.config.Config
import java.util.ServiceLoader
import org.slf4j.LoggerFactory
import scala.util.{Failure, Success}

object ProvidersLoader {
  private val logger        = LoggerFactory.getLogger(ProvidersLoader.getClass)
  private val serviceLoader = ServiceLoader.load(classOf[ServiceDiscoveryProvider])

  def load(configuration: Config, callback: String => Callback): Unit =
    serviceLoader.iterator.forEachRemaining { provider =>
      provider.initialise(configuration, callback(provider.name)) match {
        case Failure(exception) => logger.warn("Error during initialisation of {}", provider.name, exception)
        case Success(_)         => logger.debug("{} initialised successfully", provider.name)
      }
    }

}
