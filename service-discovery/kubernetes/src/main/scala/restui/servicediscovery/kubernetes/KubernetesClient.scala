package restui.servicediscovery.kubernetes
import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

import akka.actor.ActorSystem
import akka.stream.scaladsl.{Concat, Sink, Source}
import org.slf4j.LoggerFactory
import restui.servicediscovery.Models._
import restui.servicediscovery.ServiceDiscoveryProvider
import skuber._
import skuber.api.Configuration
import skuber.api.client.{EventType, WatchEvent}
import skuber.json.format._

class KubernetesClient(private val settings: Settings, private val callback: ServiceDiscoveryProvider.Callback) {
  private val logger                              = LoggerFactory.getLogger(classOf[KubernetesClient])
  implicit val system: ActorSystem                = ActorSystem("KubernetesClient")
  implicit val executionContent: ExecutionContext = system.dispatcher

  val sink = Sink.foreach[K8SWatchEvent[Service]] { event =>
    val service = event._object
    getLabels(service.metadata.labels).foreach {
      case Labels(protocol, port, swaggerPath) =>
        val serviceName = service.name
        val address     = s"$protocol://${service.copySpec.clusterIP}:$port$swaggerPath"
        val events = event._type match {
          case EventType.ADDED    => List(Up(Endpoint(serviceName, address)))
          case EventType.DELETED  => List(Down(Endpoint(serviceName, address)))
          case EventType.MODIFIED => List(Down(Endpoint(serviceName, address)), Up(Endpoint(serviceName, address)))
          case EventType.ERROR =>
            logger.warn("Service: {} errored", service.name)
            List.empty
        }
        events.foreach(callback)
    }
  }

  def listCurrentAndFutureEndpoints: Unit =
    Configuration.inClusterConfig match {
      case Failure(e) => logger.warn("Error", e)
      case Success(configuration) =>
        val k8s = k8sInit(configuration)
        for {
          list <- k8s.listByNamespace[ServiceList]
          latestVersions = list.map {
            case (_, services) =>
              services.items.flatMap { service =>
                getLabels(service.metadata.labels).map {
                  case Labels(protocol, port, swaggerPath) =>
                    Up(Endpoint(service.name, s"$protocol://${service.copySpec.clusterIP}:$port$swaggerPath"))
                }
              }.foreach(callback)
              services.metadata.map(_.resourceVersion)
          }
          watch = latestVersions.foldLeft(Source.empty[WatchEvent[Service]]) { (source, latestVersion) =>
            Source.combine(source, k8s.watchAllContinuously[Service](sinceResourceVersion = latestVersion))(Concat(_))
          }
          done <- watch.runWith(sink)
        } yield done
    }
  private def getLabels(labels: Map[String, String]): Option[Labels] =
    for {
      port <- labels.get(settings.labels.port)
      swaggerPath = labels.get(settings.labels.swaggerPath).getOrElse("/swagger.yaml")
      protocol    = labels.get(settings.labels.protocol).getOrElse("http")
    } yield Labels(protocol, port, swaggerPath)
}
