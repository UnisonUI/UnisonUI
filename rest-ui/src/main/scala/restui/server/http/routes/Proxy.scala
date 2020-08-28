package restui.server.http.routes

import scala.concurrent.Future
import scala.util.chaining._

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.scaladsl.{Sink, Source}
import restui.server.Base64

object Proxy {
  def route(implicit actorSystem: ActorSystem): Route = handle(handler)
  private def handler(implicit actorSystem: ActorSystem): PartialFunction[HttpRequest, Future[HttpResponse]] = {
    case request @ HttpRequest(_, uri, _, _, _) if uri.path.startsWith(Uri.Path("/proxy/")) && !uri.path.tail.tail.tail.isEmpty =>
      proxy(request.withUri(request.uri.withPath(request.uri.path.tail.tail)))
  }

  private def proxy(request: HttpRequest)(implicit actorSystem: ActorSystem): Future[HttpResponse] = {
    val proxyURL =
      request.uri.path.tail.head.toString.pipe(Base64.decode)
    val path = request.uri.path.tail.tail
    val uri  = Uri(proxyURL)
    val port = uri.authority.port match {
      case 0 =>
        uri.scheme match {
          case "http"  => 80
          case "https" => 443
        }
      case port => port
    }
    val flow = uri.scheme match {
      case "http" =>
        Http().outgoingConnection(uri.authority.host.address, port)
      case "https" =>
        Http().outgoingConnectionHttps(uri.authority.host.address, port)
    }
    request
      .withUri(uri.withPath(uri.path ++ path))
      .pipe(Source.single)
      .via(flow)
      .mapAsync(1) {
        case HttpResponse(statusCode, headers, _, _) if statusCode.isRedirection =>
          val urlEncoded =
            headers
              .find(_.is("location"))
              .map(_.value)
              .get
              .trim
              .pipe(Base64.encode)
          proxy(request.withUri(request.uri.withPath(Uri.Path(s"/$urlEncoded"))))
        case response => Future.successful(response)
      }
      .runWith(Sink.head)
  }
}
