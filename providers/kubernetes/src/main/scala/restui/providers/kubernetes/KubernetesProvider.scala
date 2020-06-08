package restui.providers.kubernetes

import akka.actor.ActorSystem
import com.typesafe.config.Config
import com.typesafe.scalalogging.LazyLogging
import restui.providers.Provider

class KubernetesProvider extends Provider with LazyLogging {

  override def start(actorSystem: ActorSystem, config: Config): Provider.StreamingSource = {
    implicit val system: ActorSystem = actorSystem
    val name                         = classOf[KubernetesProvider].getCanonicalName
    val settings                     = Settings.from(config)
    logger.debug("Initialising Kubernetes provider")
    new KubernetesClient(settings).listCurrentAndFutureServices.map(name -> _)
  }

}
