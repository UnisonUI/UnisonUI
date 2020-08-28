package restui.server.http.routes

import akka.NotUsed
import akka.http.scaladsl.model.sse.ServerSentEvent
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.scaladsl.Source

object Realtime {
  import akka.http.scaladsl.marshalling.sse.EventStreamMarshalling._

  def route(eventsSource: Source[ServerSentEvent, NotUsed]): Route =
    (path("events") & get)(complete(eventsSource))
}
