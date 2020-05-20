package restui.servicediscovery.docker

import scala.util.Try

import com.typesafe.config.Config
import org.slf4j.LoggerFactory
import restui.servicediscovery.ServiceDiscoveryProvider
import akka.actor.ActorSystem
import com.github.dockerjava.core.{DefaultDockerClientConfig, DockerClientBuilder}
import com.github.dockerjava.netty.NettyDockerCmdExecFactory

class DockerProvider extends ServiceDiscoveryProvider {

  private val logger = LoggerFactory.getLogger(classOf[DockerProvider])

  override def start(actorSystem: ActorSystem, config: Config, callback: ServiceDiscoveryProvider.Callback): Try[Unit] =
    Try {
      implicit val system: ActorSystem = actorSystem
      val settings                     = Settings.from(config)

      val dockerConfig = DefaultDockerClientConfig.createDefaultConfigBuilder().withDockerHost(settings.dockerHost).build()
      val client       = DockerClientBuilder.getInstance(dockerConfig).withDockerCmdExecFactory(new NettyDockerCmdExecFactory()).build()
      logger.debug("Initialising docker provider with {}", settings)
      new DockerClient(client, settings, callback).listCurrentAndFutureEndpoints
    }

}
