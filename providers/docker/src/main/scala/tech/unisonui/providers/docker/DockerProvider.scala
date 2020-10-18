package tech.unisonui.providers.docker

import akka.NotUsed
import akka.actor.typed.ActorSystem
import akka.stream.scaladsl.Source
import com.typesafe.config.Config
import com.typesafe.scalalogging.LazyLogging
import tech.unisonui.grpc.ReflectionClientImpl
import tech.unisonui.models.ServiceEvent
import tech.unisonui.providers.Provider
import tech.unisonui.providers.docker.client.impl.HttpClient

// $COVERAGE-OFF$
class DockerProvider extends Provider with LazyLogging {

  override def start(
      actorSystem: ActorSystem[_],
      config: Config): Source[(String, ServiceEvent), NotUsed] = {
    implicit val system: ActorSystem[_] = actorSystem
    val name                            = classOf[DockerProvider].getCanonicalName
    val settings                        = Settings.from(config)
    val reflectionClient                = new ReflectionClientImpl()

    val client = new HttpClient(settings.dockerHost)

    logger.debug("Initialising docker provider")

    new DockerClient(client, reflectionClient, settings).startStreaming
      .map(name -> _)
  }

}
