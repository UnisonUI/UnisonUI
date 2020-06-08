package restui.server

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.util.{Failure, Success}

import akka.actor.ActorSystem
import akka.stream.scaladsl.Sink
import com.typesafe.scalalogging.LazyLogging
import restui.Configuration
import restui.providers.ProvidersLoader
import restui.server.http.HttpServer
import restui.server.service._

// $COVERAGE-OFF$
object Main extends App with LazyLogging {

  private val Namespace: String                   = "restui.http"
  private val config                              = Configuration.config(args.headOption)
  implicit val system                             = ActorSystem()
  implicit val executionContext: ExecutionContext = system.dispatcher

  private val (queue, eventSource) = EventSource.createEventSource.run()
  private val actorRef             = system.actorOf(ServiceActor.props(queue))

  private val httpServer = new HttpServer(actorRef, eventSource)

  ProvidersLoader
    .load(config)
    .runWith(
      Sink.actorRefWithBackpressure(actorRef,
                                    ServiceActor.Init,
                                    ServiceActor.Ack,
                                    ServiceActor.Complete,
                                    e => logger.warn("Streaming error", e)))

  val interface = config.getString(s"$Namespace.interface")
  val port      = config.getInt(s"$Namespace.port")

  httpServer.bind(interface, port).onComplete {
    case Success(binding) =>
      val address = binding.localAddress
      logger.info(s"Server online at http://${address.getHostName}:${address.getPort}/")
      sys.addShutdownHook {
        binding.terminate(hardDeadline = 3.seconds).flatMap(_ => system.terminate())
      }
    case Failure(ex) =>
      logger.error("Failed to bind HTTP endpoint, terminating system", ex)
      system.terminate().onComplete(_ => sys.exit(1))
  }

}
