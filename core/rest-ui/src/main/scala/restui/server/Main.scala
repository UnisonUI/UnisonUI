package restui.server

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.util.{Failure, Success}

import akka.actor.ActorSystem
import akka.http.scaladsl.model.sse.ServerSentEvent
import akka.stream.scaladsl.{BroadcastHub, Keep, Source}
import akka.stream.{DelayOverflowStrategy, OverflowStrategy}
import io.circe._
import io.circe.generic.auto._
import io.circe.syntax._
import org.slf4j.LoggerFactory
import restui.Configuration
import restui.server.http.{HttpServer, Models => HttpModels}
import restui.server.service.EndpointsActor
import restui.servicediscovery.Models._
import restui.servicediscovery.ProvidersLoader
object Main extends App {

  private val config                              = Configuration.config
  private val logger                              = LoggerFactory.getLogger(Main.getClass)
  implicit val system                             = ActorSystem()
  implicit val executionContext: ExecutionContext = system.dispatcher
  logger.info("{}  {}", (HttpModels.Up("a"): HttpModels.Event).asJson.noSpaces, (HttpModels.Down("b"): HttpModels.Event).asJson.noSpaces)
  val (sourceQueue, eventsSource) = Source
    .queue[(String, String)](Int.MaxValue, OverflowStrategy.backpressure)
    .delay(1.seconds, DelayOverflowStrategy.backpressure)
    .map { case input => ServerSentEvent(Encoder[(String, String)].apply(input).noSpaces) }
    .keepAlive(1.second, () => ServerSentEvent.heartbeat)
    .toMat(BroadcastHub.sink[ServerSentEvent])(Keep.both)
    .run()

  private val actorRef = system.actorOf(EndpointsActor.props(sourceQueue))

  private val httpServer = new HttpServer(actorRef, eventsSource)
  private def callback(provider: String)(event: Event): Unit =
    actorRef ! (provider -> event)
  actorRef ! ("Default"  -> Up(Endpoint("PetStore", "https://petstore.swagger.io/v2/swagger.json")))

  ProvidersLoader.load(config, callback)

  httpServer.bind(8080).onComplete {
    case Success(binding) =>
      val address = binding.localAddress
      logger.info("Server online at http://{}:{}/", address.getHostName, address.getPort)
      sys.addShutdownHook {
        binding.terminate(hardDeadline = 3.seconds).flatMap(_ => system.terminate())
      }
    case Failure(ex) =>
      logger.error("Failed to bind HTTP endpoint, terminating system", ex)
      system.terminate().onComplete(_ => sys.exit(1))
  }
}
