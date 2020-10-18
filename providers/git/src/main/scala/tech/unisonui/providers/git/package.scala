package tech.unisonui.providers

import akka.NotUsed
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.stream.scaladsl.{Flow => AkkaFlow, Source => AkkaSource}

import scala.concurrent.Future

package object git {
  type Flow[I, O]      = AkkaFlow[I, O, NotUsed]
  type Source[T]       = AkkaSource[T, _]
  type RequestExecutor = HttpRequest => Future[HttpResponse]
}
