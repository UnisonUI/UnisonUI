package restui.servicediscovery.docker
import scala.collection.mutable
import scala.jdk.CollectionConverters._

import com.github.dockerjava.api.model.{ContainerNetwork, Event}
import com.github.dockerjava.core.command.EventsResultCallback
import com.github.dockerjava.core.{DefaultDockerClientConfig, DockerClientBuilder}
import com.github.dockerjava.netty.NettyDockerCmdExecFactory
import restui.servicediscovery.Models._
import restui.servicediscovery.ServiceDiscoveryProvider

class DockerClient(private val settings: Settings, private val callback: ServiceDiscoveryProvider.Callback) {

  private val dockerConfig = DefaultDockerClientConfig.createDefaultConfigBuilder().withDockerHost(settings.dockerHost).build()
  private val client       = DockerClientBuilder.getInstance(dockerConfig).withDockerCmdExecFactory(new NettyDockerCmdExecFactory()).build()

  def listCurrentAndFutureEndpoints: Unit = {
    listRunningEndpoints
    listenForNewEnpoints
  }
  private def listenForNewEnpoints =
    client
      .eventsCmd()
      .withEventFilter("start", "stop", "kill")
      .exec(new EventsResultCallback {
        override def onNext(event: Event): Unit = {
          val container = client.inspectContainerCmd(event.getId()).exec
          val labels    = container.getConfig.getLabels.asScala
          val networks  = container.getNetworkSettings.getNetworks.asScala
          findEndpoint(labels, networks)
            .map(endpoint =>
              if (event.getStatus == "start") Up(endpoint)
              else Down(endpoint))
            .foreach(callback)
          super.onNext(event)
        }
      })

  private def listRunningEndpoints =
    client
      .listContainersCmd()
      .withStatusFilter(Seq("running").asJavaCollection)
      .exec()
      .asScala
      .toList
      .flatMap { container =>
        val labels   = container.getLabels.asScala
        val networks = container.getNetworkSettings.getNetworks.asScala
        findEndpoint(labels, networks).map(Up)
      }
      .foreach(callback)

  private def findEndpoint(labels: mutable.Map[String, String], networks: mutable.Map[String, ContainerNetwork]): Option[Endpoint] =
    for {
      labels    <- findMatchingLabels(labels)
      ipAddress <- findFirstIpAddress(networks)
    } yield Endpoint(labels.serviceName, s"http://$ipAddress:${labels.port.toInt}${labels.swaggerPath}")

  private def findFirstIpAddress(networks: mutable.Map[String, ContainerNetwork]): Option[String] =
    networks.toList.headOption.map(_._2.getIpAddress)

  private def findMatchingLabels(labels: mutable.Map[String, String]): Option[Labels] =
    for {
      serviceName <- labels.get(settings.labels.serviceName)
      port        <- labels.get(settings.labels.port)
      swaggerPath = labels.get(settings.labels.swaggerPath).getOrElse("/swagger.yaml")
    } yield Labels(serviceName, port, swaggerPath)
}
