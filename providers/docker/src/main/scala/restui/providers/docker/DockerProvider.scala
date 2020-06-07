package restui.providers.docker

import scala.util.Try

import akka.actor.ActorSystem
import akka.stream.scaladsl.Sink
import com.typesafe.config.Config
import com.typesafe.scalalogging.LazyLogging
import restui.providers.Provider
import restui.providers.docker.client.impl.HttpClient

class DockerProvider extends Provider with LazyLogging {

  override def start(actorSystem: ActorSystem, config: Config, callback: Provider.Callback): Try[Unit] =
    Try {
      implicit val system: ActorSystem = actorSystem
      val settings                     = Settings.from(config)

      val client = new HttpClient(settings.dockerHost)

      logger.debug("Initialising docker provider")

      new DockerClient(client, settings).startStreaming.runWith(Sink.foreach(callback))
    }

}
