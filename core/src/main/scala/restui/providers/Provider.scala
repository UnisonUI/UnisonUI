package restui.providers

import scala.util.Try

import akka.actor.ActorSystem
import com.typesafe.config.Config
import restui.models.ServiceEvent

trait Provider {
  def start(actorSystem: ActorSystem, config: Config, callback: Provider.Callback): Try[Unit]
}

object Provider {
  type Callback = ServiceEvent => Unit
}
