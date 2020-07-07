package restui.server

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.util.{Failure, Success}

import akka.actor.ActorSystem
import akka.stream.scaladsl.{Sink, Source}
import com.typesafe.scalalogging.LazyLogging
import restui.Configuration
import restui.models.{Metadata, Service, ServiceEvent}
import restui.providers.ProvidersLoader
import restui.server.http.HttpServer
import restui.server.service._

// $COVERAGE-OFF$
object Main extends App with LazyLogging {

  private val Namespace: String                   = "restui.http"
  private val config                              = Configuration.config(args.headOption)
  implicit val system                             = ActorSystem()
  implicit val executionContext: ExecutionContext = system.dispatcher

  val interface         = config.getString(s"$Namespace.interface")
  val port              = config.getInt(s"$Namespace.port")
  val selfSpecification = config.getBoolean("restui.self-specification")

  private val (queue, eventSource) = EventSource.createEventSource.run()
  private val actorRef             = system.actorOf(ServiceActor.props(queue))

  private val httpServer = new HttpServer(actorRef, eventSource)

  val specificationSource = if (selfSpecification) {
    val specification = scala.io.Source.fromResource("specification.yaml").getLines().mkString("\n")
    Source.single(
      Main.getClass.getCanonicalName -> ServiceEvent.ServiceUp(
        Service("restui:restui", "RestUI", specification, Map(Metadata.File -> "specification.yaml"))))
  } else Source.empty[(String, ServiceEvent)]

  ProvidersLoader
    .load(config)
    .prepend(specificationSource)
    .runWith(
      Sink.actorRefWithBackpressure(actorRef,
                                    ServiceActor.Init,
                                    ServiceActor.Ack,
                                    ServiceActor.Complete,
                                    e => logger.warn("Streaming error", e)))

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
