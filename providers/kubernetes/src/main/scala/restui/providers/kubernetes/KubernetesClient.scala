package restui.providers.kubernetes
import akka.NotUsed
import akka.actor.typed.ActorSystem
import akka.stream.OverflowStrategy
import akka.stream.scaladsl.{BroadcastHub, Keep, Sink, Source}
import com.typesafe.scalalogging.LazyLogging
import restui.grpc.ReflectionClient
import restui.models.ServiceEvent
import restui.providers.kubernetes.ServiceActor.Message
import skuber._
import skuber.api.Configuration
import skuber.json.format._

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.util.{Failure, Success}

class KubernetesClient(private val settings: Settings,
                       private val reflectionClient: ReflectionClient)(implicit
    system: ActorSystem[_])
    extends LazyLogging {
  implicit private val client: ReflectionClient = reflectionClient
  implicit private val executionContext: ExecutionContext =
    system.executionContext

  private val BufferSize = 10

  private val (queue, source) =
    Source
      .queue[ServiceEvent](BufferSize, OverflowStrategy.backpressure)
      .toMat(BroadcastHub.sink[ServiceEvent])(Keep.both)
      .run()

  private val serviceActorRef =
    system.systemActorOf(ServiceActor(settings.labels, queue),
                         "kubernetesServiceActor")

  def listCurrentAndFutureServices: Source[ServiceEvent, NotUsed] =
    Configuration.inClusterConfig match {
      case Failure(e) =>
        logger.warn("Couldn't connect to the Kubernetes cluster", e)
        Source.empty
      case Success(configuration) =>
        val k8s = k8sInit(configuration)(system.classicSystem)
        Source
          .tick(1.second, settings.pollingInterval, ())
          .flatMapConcat { _ =>
            Source.futureSource {
              k8s
                .listByNamespace[ServiceList]()
                .map { map =>
                  Source(map.view.mapValues(_.items).toList)
                }
                .recover { case e =>
                  logger.warn("Error while fetching services", e)
                  Source.empty[(String, List[Service])]
                }

            }
          }
          .runWith(
            Sink.foreach { case (namespace, services) =>
              serviceActorRef ! Message.Add(namespace, services)
            }
          )
        source
    }
}
