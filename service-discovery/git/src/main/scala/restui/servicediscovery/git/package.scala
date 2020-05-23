package restui.servicediscovery

import scala.concurrent.Future

import akka.NotUsed
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.stream.scaladsl.{Flow, Source}

package object git {
  type F[I, O]         = Flow[I, O, NotUsed]
  type S[T]            = Source[T, NotUsed]
  type RequestExecutor = HttpRequest => Future[HttpResponse]
}
