package restui.providers.git

import scala.concurrent.ExecutionContext

import akka.NotUsed
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import com.typesafe.config.Config
import com.typesafe.scalalogging.LazyLogging
import restui.models.ServiceEvent
import restui.providers.Provider
import restui.providers.git.settings.Settings
import restui.providers.git.vcs.VCS

// $COVERAGE-OFF$
class GitProvider extends Provider with LazyLogging {
  override def start(actorSystem: ActorSystem, config: Config): Provider.StreamingSource = {
    val name                                        = classOf[GitProvider].getCanonicalName
    implicit val system: ActorSystem                = actorSystem
    implicit val executionContext: ExecutionContext = actorSystem.dispatcher
    val settings                                    = Settings.from(config)

    logger.debug("Initialising git provider")

    VCS
      .source(settings, Http().singleRequest(_))
      .mapMaterializedValue(_ => NotUsed)
      .map(service => name -> ServiceEvent.ServiceUp(service))
  }

}
