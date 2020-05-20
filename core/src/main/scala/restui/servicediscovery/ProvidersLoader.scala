package restui.servicediscovery

import java.util.ServiceLoader

import scala.jdk.CollectionConverters._
import scala.util.{Failure, Success}

import com.typesafe.config.Config
import org.slf4j.LoggerFactory
import restui.servicediscovery.ServiceDiscoveryProvider.Callback
import akka.actor.ActorSystem

object ProvidersLoader {
  private val logger        = LoggerFactory.getLogger(ProvidersLoader.getClass)
  private val serviceLoader = ServiceLoader.load(classOf[ServiceDiscoveryProvider])

  def load(configuration: Config, callback: String => Callback)(implicit system: ActorSystem): Unit = {
    val providers = configuration.getStringList("restui.providers").asScala.toList

    logger.debug("List of providers: {}", providers.mkString(", "))

    serviceLoader.iterator.asScala
      .filter(provider => providers.contains(provider.getClass.getCanonicalName))
      .foreach { provider =>
        val name = provider.getClass.getCanonicalName
        provider.start(system, configuration, callback(name)) match {
          case Failure(exception) => logger.warn("Error during initialisation of {}", name, exception)
          case Success(_)         => logger.debug("{} initialised successfully", name)
        }
      }
  }
}
