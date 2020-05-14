package restui.server

import akka.actor.ActorSystem
import restui.Configuration
import restui.server.http.HttpServer
import restui.server.service.EndpointsActor
import restui.servicediscovery.ProvidersLoader
import restui.servicediscovery.Models.Event
import org.slf4j.LoggerFactory

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

object Main extends App {

  private val config                              = Configuration.config
  private val logger                              = LoggerFactory.getLogger(Main.getClass)
  implicit val system                             = ActorSystem()
  implicit val executionContext: ExecutionContext = system.dispatcher

  private val actorRef = system.actorOf(EndpointsActor.props)

  private val httpServer = new HttpServer(actorRef)
  private def callback(provider: String)(event: Event): Unit =
    actorRef ! (provider -> event)

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
