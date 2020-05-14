package restui.server.http

import akka.actor.{ActorRef, ActorSystem}
import akka.pattern.ask
import akka.util.Timeout

import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport
import io.circe.generic.auto._
import restui.server.http.templates._
import restui.server.http.templates.html._
import restui.server.http.templates.css.MainStyle
import restui.server.service.EndpointsActor.Get
import restui.servicediscovery.Models.Endpoint

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContextExecutor, Future}

class HttpServer(private val endpointsActorRef: ActorRef)(implicit actorSystem: ActorSystem)
    extends Directives
    with FailFastCirceSupport
    with ScalaTagsSupport
    with ScalaCssSupport {
  implicit val timeout: Timeout                                   = 5.seconds
  implicit private val executionContext: ExecutionContextExecutor = actorSystem.dispatcher

  def bind(port: Int): Future[Http.ServerBinding] =
    Http().bindAndHandle(routes, "0.0.0.0", port)

  private val routes = get {
    path(separateOnSlashes(Assets.style(MainStyle).stripPrefix("/"))) {
      complete(MainStyle)
    } ~
      path(PathEnd)(complete(Index.template)) ~
      path("endpoints") {
        complete((endpointsActorRef ? Get).mapTo[List[Endpoint]])
      }
  }
}
