package restui.servicediscovery

import java.util.ServiceLoader

import scala.jdk.CollectionConverters._
import scala.util.{Failure, Success, Try}

import akka.actor.ActorSystem
import com.typesafe.config.Config
import com.typesafe.scalalogging.LazyLogging
import restui.servicediscovery.ServiceDiscoveryProvider.Callback

object ProvidersLoader extends LazyLogging {
  ServiceLoader.load(classOf[ServiceDiscoveryProvider])

  def load(configuration: Config, callback: String => Callback)(implicit system: ActorSystem): Unit = {
    val providers = configuration.getStringList("restui.providers").asScala.toList

    logger.debug("List of providers: {}", providers.mkString(", "))
    providers.flatMap { classname =>
      Try {
        val classInstance = Class.forName(classname);
        classInstance.getDeclaredConstructor().newInstance().asInstanceOf[ServiceDiscoveryProvider]
      }.toOption
    }.foreach { provider =>
      val name = provider.getClass.getCanonicalName
      provider.start(system, configuration, callback(name)) match {
        case Failure(exception) => logger.warn(s"Error during initialisation of $name", exception)
        case Success(_)         => logger.debug(s"$name initialised successfully")
      }

    }
  }
}
