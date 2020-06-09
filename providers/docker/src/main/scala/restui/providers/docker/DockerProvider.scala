package restui.providers.docker

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.scaladsl.Source
import com.typesafe.config.Config
import com.typesafe.scalalogging.LazyLogging
import restui.models.ServiceEvent
import restui.providers.Provider
import restui.providers.docker.client.impl.HttpClient

// $COVERAGE-OFF$
class DockerProvider extends Provider with LazyLogging {

  override def start(actorSystem: ActorSystem, config: Config): Source[(String, ServiceEvent), NotUsed] = {
    implicit val system: ActorSystem = actorSystem
    val name                         = classOf[DockerProvider].getCanonicalName
    val settings                     = Settings.from(config)

    val client = new HttpClient(settings.dockerHost)

    logger.debug("Initialising docker provider")

    new DockerClient(client, settings).startStreaming.map(name -> _)
  }

}
