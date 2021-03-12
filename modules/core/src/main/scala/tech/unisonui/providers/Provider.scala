package tech.unisonui.providers

import akka.NotUsed
import akka.actor.typed.ActorSystem
import akka.stream.scaladsl.Source
import com.typesafe.config.Config
import tech.unisonui.models.ServiceEvent

trait Provider {
  def start(actorSystem: ActorSystem[_],
            config: Config): Source[(String, ServiceEvent), NotUsed]
}
