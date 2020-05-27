package restui.providers.kubernetes

import scala.util.Try

import akka.actor.ActorSystem
import com.typesafe.config.Config
import com.typesafe.scalalogging.LazyLogging
import restui.providers.Provider

class KubernetesProvider extends Provider with LazyLogging {

  override def start(actorSystem: ActorSystem, config: Config, callback: Provider.Callback): Try[Unit] =
    Try {
      implicit val system: ActorSystem = actorSystem
      val settings                     = Settings.from(config)
      logger.debug("Initialising Kubernetes provider")
      new KubernetesClient(settings, callback).listCurrentAndFutureServices
    }

}
