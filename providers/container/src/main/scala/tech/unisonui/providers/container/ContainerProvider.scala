package tech.unisonui.providers.container

import akka.NotUsed
import akka.actor.typed.ActorSystem
import akka.stream.scaladsl.{Merge, Source}
import com.typesafe.config.Config
import com.typesafe.scalalogging.LazyLogging
import tech.unisonui.grpc.ReflectionClientImpl
import tech.unisonui.models.ServiceEvent
import tech.unisonui.providers.Provider
import tech.unisonui.providers.container.docker.client.impl.HttpClient
import tech.unisonui.providers.container.settings.Settings
import tech.unisonui.providers.container.sources.{
  DockerSource,
  KubernetesSource
}
// $COVERAGE-OFF$
class ContainerProvider extends Provider with LazyLogging {

  override def start(
      actorSystem: ActorSystem[_],
      config: Config): Source[(String, ServiceEvent), NotUsed] = {
    implicit val system: ActorSystem[_] = actorSystem
    val name                            = classOf[ContainerProvider].getCanonicalName
    val settings                        = Settings.from(config)
    val reflectionClient                = new ReflectionClientImpl()
    val dockerSource = settings.docker.fold(Source.empty[ServiceEvent]) {
      setting =>
        val client = new HttpClient(setting.host)
        new DockerSource(client, reflectionClient, settings).startStreaming
    }
    val kubernetesSource =
      settings.kubernetes.fold(Source.empty[ServiceEvent]) { setting =>
        new KubernetesSource(setting.pollingInterval,
                             settings.labels,
                             reflectionClient).listCurrentAndFutureServices
      }
    logger.debug("Initialising container provider")
    Source
      .combine(dockerSource, kubernetesSource)(Merge(_))
      .map(name -> _)
  }

}
