package tech.unisonui.server.service

import akka.NotUsed
import akka.http.scaladsl.model.sse.ServerSentEvent
import akka.stream.scaladsl.{
  BroadcastHub,
  Keep,
  RunnableGraph,
  Source,
  SourceQueueWithComplete
}
import akka.stream.{DelayOverflowStrategy, OverflowStrategy}
import io.circe.syntax._
import tech.unisonui.models.Event

import scala.concurrent.duration._

object EventSource {
  def createEventSource: RunnableGraph[(SourceQueueWithComplete[Event],
                                        Source[ServerSentEvent, NotUsed])] =
    Source
      .queue[Event](Int.MaxValue, OverflowStrategy.backpressure)
      .delay(1.seconds, DelayOverflowStrategy.backpressure)
      .map(event => ServerSentEvent(event.asJson.noSpaces))
      .keepAlive(5.seconds, () => ServerSentEvent.heartbeat)
      .toMat(BroadcastHub.sink[ServerSentEvent])(Keep.both)

}
