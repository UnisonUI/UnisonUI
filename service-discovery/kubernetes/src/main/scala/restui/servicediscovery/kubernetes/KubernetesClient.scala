package restui.servicediscovery.kubernetes
import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.util.{Failure, Success}

import akka.actor.{ActorSystem, Props}
import akka.stream.scaladsl.{Sink, Source}
import org.slf4j.LoggerFactory
import restui.servicediscovery.ServiceDiscoveryProvider
import skuber._
import skuber.api.Configuration
import skuber.json.format._
class KubernetesClient(private val settings: Settings, private val callback: ServiceDiscoveryProvider.Callback) {
  private val logger                              = LoggerFactory.getLogger(classOf[KubernetesClient])
  implicit val system: ActorSystem                = ActorSystem("KubernetesClient")
  implicit val executionContent: ExecutionContext = system.dispatcher
  private val endpointsActorRef                   = system.actorOf(Props(classOf[EndpointsActor], settings.labels, callback))
  def listCurrentAndFutureEndpoints: Unit =
    Configuration.inClusterConfig match {
      case Failure(e) => logger.warn("Couldn't connect to the Kubernetes cluster", e)
      case Success(configuration) =>
        val k8s = k8sInit(configuration)
        val source = Source
          .tick(1.second, settings.pollingInterval, ())
          .flatMapConcat { _ =>
            Source.futureSource {
              k8s.listByNamespace[ServiceList].map { map =>
                Source(map.view.mapValues(_.items).toList)
              }
            }.recover {
              case e =>
                logger.warn("Error while fetching services", e)
                Source.empty[List[Service]]
            }
          }

        source.runWith(
          Sink
            .actorRefWithBackpressure(endpointsActorRef,
                                      EndpointsActor.Init,
                                      EndpointsActor.Ack,
                                      EndpointsActor.Complete,
                                      e => logger.warn("Stream error", e)))
    }
}
