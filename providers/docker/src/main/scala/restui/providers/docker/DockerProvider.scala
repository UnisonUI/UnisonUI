package restui.providers.docker

import akka.actor.ActorSystem
import com.typesafe.config.Config
import com.typesafe.scalalogging.LazyLogging
import restui.providers.Provider
import restui.providers.docker.client.impl.HttpClient

class DockerProvider extends Provider with LazyLogging {

  override def start(actorSystem: ActorSystem, config: Config): Provider.StreamingSource = {
    implicit val system: ActorSystem = actorSystem
    val name                         = classOf[DockerProvider].getCanonicalName
    val settings                     = Settings.from(config)

    val client = new HttpClient(settings.dockerHost)

    logger.debug("Initialising docker provider")

    new DockerClient(client, settings).startStreaming.map(name -> _)
  }

}
