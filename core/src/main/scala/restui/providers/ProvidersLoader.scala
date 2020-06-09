package restui.providers

import scala.jdk.CollectionConverters._
import scala.util.{Failure, Success, Try}

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.scaladsl.{Merge, Source}
import com.typesafe.config.Config
import com.typesafe.scalalogging.LazyLogging
import restui.models.ServiceEvent

object ProvidersLoader extends LazyLogging {
  def load(configuration: Config)(implicit system: ActorSystem): Source[(String, ServiceEvent), NotUsed] = {
    val providers = configuration.getStringList("restui.providers").asScala.toList

    logger.debug("List of providers: {}", providers.mkString(", "))
    providers.flatMap { classname =>
      Try {
        val classInstance = Class.forName(classname)
        classInstance.getDeclaredConstructor().newInstance().asInstanceOf[Provider]
      }.toOption
    }.foldLeft(Source.empty[(String, ServiceEvent)]) { (source, provider) =>
      val name = provider.getClass.getCanonicalName
      Try(provider.start(system, configuration)) match {
        case Failure(exception) =>
          logger.warn(s"Error during initialisation of $name", exception)
          source
        case Success(providerSource) =>
          logger.debug(s"$name initialised successfully")
          Source.combine(source, providerSource)(Merge(_))
      }
    }
  }
}
