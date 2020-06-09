package restui.providers.webhook

import scala.concurrent.ExecutionContext
import scala.util.Try

import akka.actor.ActorSystem
import com.typesafe.config.Config
import com.typesafe.scalalogging.LazyLogging
import restui.providers.Provider
import restui.providers.webhook.settings.Settings

class WebhookProvider extends Provider with LazyLogging {
  override def start(actorSystem: ActorSystem, config: Config, callback: Provider.Callback): Try[Unit] =
    Try {
      implicit val system: ActorSystem                = actorSystem
      implicit val executionContext: ExecutionContext = actorSystem.dispatcher
      val settings                                    = Settings.from(config)

      logger.debug("Initialising webhook provider")
      HttpServer.bind(settings.interface, settings.port, callback)
    }

}
