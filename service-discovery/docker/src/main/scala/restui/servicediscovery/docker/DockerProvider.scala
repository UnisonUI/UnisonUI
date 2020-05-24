package restui.servicediscovery.docker

import scala.util.Try

import akka.actor.ActorSystem
import com.github.dockerjava.core.{DefaultDockerClientConfig, DockerClientBuilder}
import com.github.dockerjava.netty.NettyDockerCmdExecFactory
import com.typesafe.config.Config
import com.typesafe.scalalogging.LazyLogging
import restui.servicediscovery.ServiceDiscoveryProvider

class DockerProvider extends ServiceDiscoveryProvider with LazyLogging {

  override def start(actorSystem: ActorSystem, config: Config, callback: ServiceDiscoveryProvider.Callback): Try[Unit] =
    Try {
      implicit val system: ActorSystem = actorSystem
      val settings                     = Settings.from(config)

      val dockerConfig = DefaultDockerClientConfig.createDefaultConfigBuilder().withDockerHost(settings.dockerHost).build()
      val client       = DockerClientBuilder.getInstance(dockerConfig).withDockerCmdExecFactory(new NettyDockerCmdExecFactory()).build()

      logger.debug("Initialising docker provider")

      new DockerClient(client, settings, callback).listCurrentAndFutureEndpoints
    }

}
