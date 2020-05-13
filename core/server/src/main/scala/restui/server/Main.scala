package restui.server

import akka.actor.ActorSystem
import restui.Configuration
import restui.server.http.HttpServer
import restui.server.service.EndpointsActor
import restui.servicediscovery.ProvidersLoader
import restui.servicediscovery.Models.Event

object Main extends App {
  implicit val system  = ActorSystem()
  private val actorRef = system.actorOf(EndpointsActor.props)
  private val config   = Configuration.config

  private val httpServer = new HttpServer(actorRef)
  private def callback(provider: String)(event: Event): Unit =
    actorRef ! (provider -> event)

  ProvidersLoader.load(config, callback)
  httpServer.bind(8080)
}
