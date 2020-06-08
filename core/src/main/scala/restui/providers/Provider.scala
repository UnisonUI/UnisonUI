package restui.providers

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.scaladsl.Source
import com.typesafe.config.Config
import restui.models.ServiceEvent

trait Provider {
  def start(actorSystem: ActorSystem, config: Config): Provider.StreamingSource
}

object Provider {
  type StreamingSource = Source[(String, ServiceEvent), NotUsed]
}
