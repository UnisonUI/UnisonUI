package restui.providers.kubernetes

import akka.NotUsed
import akka.actor.typed.ActorSystem
import akka.stream.scaladsl.Source
import com.typesafe.config.Config
import com.typesafe.scalalogging.LazyLogging
import restui.models.ServiceEvent
import restui.providers.Provider

// $COVERAGE-OFF$
class KubernetesProvider extends Provider with LazyLogging {

  override def start(
      actorSystem: ActorSystem[_],
      config: Config): Source[(String, ServiceEvent), NotUsed] = {
    implicit val system: ActorSystem[_] = actorSystem
    val name                            = classOf[KubernetesProvider].getCanonicalName
    val settings                        = Settings.from(config)
    logger.debug("Initialising Kubernetes provider")
    new KubernetesClient(settings).listCurrentAndFutureServices.map(name -> _)
  }

}
