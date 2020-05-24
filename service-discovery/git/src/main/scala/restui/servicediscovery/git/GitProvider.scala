package restui.servicediscovery.git

import scala.concurrent.ExecutionContext
import scala.util.Try

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.scaladsl.Sink
import com.typesafe.config.Config
import com.typesafe.scalalogging.LazyLogging
import restui.servicediscovery.ServiceDiscoveryProvider
import restui.servicediscovery.git.settings.Settings
import restui.servicediscovery.git.vcs.VCS
import restui.servicediscovery.models.ServiceUp

class GitProvider extends ServiceDiscoveryProvider with LazyLogging {
  override def start(actorSystem: ActorSystem, config: Config, callback: ServiceDiscoveryProvider.Callback): Try[Unit] =
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
