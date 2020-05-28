package restui.providers.git

import scala.concurrent.ExecutionContext
import scala.util.Try

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.scaladsl.Sink
import com.typesafe.config.Config
import com.typesafe.scalalogging.LazyLogging
import restui.providers.Provider
import restui.providers.git.settings.Settings
import restui.providers.git.vcs.VCS
import restui.models.ServiceUp

class GitProvider extends Provider with LazyLogging {
  override def start(actorSystem: ActorSystem, config: Config, callback: Provider.Callback): Try[Unit] =
    Try {
      implicit val system: ActorSystem                = actorSystem
      implicit val executionContext: ExecutionContext = actorSystem.dispatcher
      val settings                                    = Settings.from(config)

      logger.debug("Initialising git provider")

      VCS
        .source(settings, Http().singleRequest(_))
        .to(Sink.foreach(service => callback(ServiceUp(service))))
        .run()
    }

}
