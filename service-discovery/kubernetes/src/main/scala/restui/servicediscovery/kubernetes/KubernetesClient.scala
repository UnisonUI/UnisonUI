package restui.servicediscovery.kubernetes
import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.util.{Failure, Success}

import akka.actor.{ActorSystem, Props}
import akka.stream.scaladsl.{Sink, Source}
import com.typesafe.scalalogging.LazyLogging
import restui.servicediscovery.ServiceDiscoveryProvider
import skuber._
import skuber.api.Configuration
import skuber.json.format._

class KubernetesClient(private val settings: Settings, private val callback: ServiceDiscoveryProvider.Callback)(implicit
    system: ActorSystem)
    extends LazyLogging {
  implicit val executionContent: ExecutionContext = system.dispatcher
  private val serviceActorRef                     = system.actorOf(Props(classOf[ServiceActor], settings.labels, callback))

  def listCurrentAndFutureServices: Unit =
    Configuration.inClusterConfig match {
      case Failure(e) => logger.warn("Couldn't connect to the Kubernetes cluster", e)
      case Success(configuration) =>
        val k8s = k8sInit(configuration)
        Source
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
          .runWith(
            Sink
              .actorRefWithBackpressure(serviceActorRef,
                                        ServiceActor.Init,
                                        ServiceActor.Ack,
                                        ServiceActor.Complete,
                                        e => logger.warn("Streaming error", e)))
    }
}
