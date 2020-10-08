package restui.providers.git

import scala.concurrent.ExecutionContext

import akka.NotUsed
import akka.actor.typed.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.scaladsl.Source
import com.typesafe.config.Config
import com.typesafe.scalalogging.LazyLogging
import restui.models.ServiceEvent
import restui.providers.Provider
import restui.providers.git.settings.Settings
import restui.providers.git.vcs.VCS

// $COVERAGE-OFF$
class GitProvider extends Provider with LazyLogging {
  override def start(actorSystem: ActorSystem[_], config: Config): Source[(String, ServiceEvent), NotUsed] = {
    val name                                        = classOf[GitProvider].getCanonicalName
    implicit val system: ActorSystem[_]             = actorSystem
    implicit val executionContext: ExecutionContext = actorSystem.executionContext
    val settings                                    = Settings.from(config)

    logger.debug("Initialising git provider")

    VCS
      .source(settings, Http().singleRequest(_))
      .mapMaterializedValue(_ => NotUsed)
      .map(event => name -> event)
  }

}
