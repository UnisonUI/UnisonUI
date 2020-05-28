package restui.providers.docker
import scala.collection.mutable
import scala.concurrent.ExecutionContext
import scala.jdk.CollectionConverters._

import akka.NotUsed
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.unmarshalling.Unmarshaller
import akka.stream.OverflowStrategy
import akka.stream.scaladsl.{Source, _}
import com.github.dockerjava.api.model.{ContainerNetwork, Event}
import com.github.dockerjava.api.{DockerClient => JDockerClient}
import com.github.dockerjava.core.command.EventsResultCallback
import com.typesafe.scalalogging.LazyLogging
import restui.providers.Provider
import restui.models._

class DockerClient(private val client: JDockerClient, private val settings: Settings, private val callback: Provider.Callback)(implicit
    val system: ActorSystem)
    extends LazyLogging {
  import DockerClient._
  implicit val executionContent: ExecutionContext = system.dispatcher

  private def createQueue: SourceQueueWithComplete[ServiceEvent] =
    Source
      .queue(BufferSize, OverflowStrategy.backpressure)
      .flatMapConcat(downloadFile)
      .toMat(Sink.foreach[ServiceEvent](callback))(Keep.left)
      .run()

  private def downloadFile(event: ServiceEvent): Source[ServiceEvent, NotUsed] =
    event match {
      case serviceDown: ServiceDown => Source.single(serviceDown)
      case ServiceUp(service) =>
        Source.futureSource {
          Http()
            .singleRequest(HttpRequest(uri = service.file.content))
            .flatMap { response =>
              Unmarshaller.stringUnmarshaller(response.entity)
            }
            .map { content =>
              Source.single(ServiceUp(service.copy(file = service.file.copy(content = content))))
            }
            .recover { throwable =>
              logger.warn("There was an error while download the file", throwable)
              Source.empty[ServiceEvent]
            }
        }.mapMaterializedValue(_ => NotUsed)
    }

  def listCurrentAndFutureEndpoints: Unit = {
    val queue = createQueue
    listRunningEndpoints(queue)
    listenForNewEnpoints(queue)
  }

  private def listenForNewEnpoints(queue: SourceQueueWithComplete[ServiceEvent]) =
    client
      .eventsCmd()
      .withEventFilter(StartFilter, StopFilter, KillFilter)
      .exec(new EventsResultCallback {
        override def onNext(event: Event): Unit = {
          val container = client.inspectContainerCmd(event.getId()).exec
          val labels    = container.getConfig.getLabels.asScala
          val networks  = container.getNetworkSettings.getNetworks.asScala
          findEndpoint(labels, networks).map {
            case (serviceName, address) =>
              if (event.getStatus == StartFilter) ServiceUp(Service(serviceName, OpenApiFile(ContentType.fromString(address), address)))
              else ServiceDown(serviceName)
          }.foreach(queue.offer)
          super.onNext(event)
        }
      })

  private def listRunningEndpoints(queue: SourceQueueWithComplete[ServiceEvent]) =
    client
      .listContainersCmd()
      .withStatusFilter(Seq(RunningFilter).asJavaCollection)
      .exec()
      .asScala
      .toList
      .flatMap { container =>
        val labels   = container.getLabels.asScala
        val networks = container.getNetworkSettings.getNetworks.asScala
        findEndpoint(labels, networks).map {
          case (serviceName, address) => ServiceUp(Service(serviceName, OpenApiFile(ContentType.fromString(address), address)))
        }
      }
      .foreach(queue.offer)

  private def findEndpoint(labels: mutable.Map[String, String],
                           networks: mutable.Map[String, ContainerNetwork]): Option[ServiceNameWithAddress] =
    for {
      labels    <- findMatchingLabels(labels)
      ipAddress <- findFirstIpAddress(networks)
    } yield (labels.serviceName, s"http://$ipAddress:${labels.port.toInt}${labels.swaggerPath}")

  private def findFirstIpAddress(networks: mutable.Map[String, ContainerNetwork]): Option[String] =
    networks.toList.headOption.map(_._2.getIpAddress)

  private def findMatchingLabels(labels: mutable.Map[String, String]): Option[Labels] =
    for {
      serviceName <- labels.get(settings.labels.serviceName)
      port        <- labels.get(settings.labels.port)
      swaggerPath = labels.get(settings.labels.swaggerPath).getOrElse("/swagger.yaml")
    } yield Labels(serviceName, port, swaggerPath)
}

object DockerClient {
  private type ServiceNameWithAddress = (String, String)
  private val BufferSize: Int = 10
  private val StartFilter     = "start"
  private val RunningFilter   = "running"
  private val StopFilter      = "stop"
  private val KillFilter      = "kill"
}
