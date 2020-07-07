package restui.providers.webhook

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.scaladsl.Source
import com.typesafe.config.Config
import com.typesafe.scalalogging.LazyLogging
import restui.models.{Metadata, Service, ServiceEvent}
import restui.providers.Provider
import restui.providers.webhook.settings.Settings

// $COVERAGE-OFF$
class WebhookProvider extends Provider with LazyLogging {
  override def start(actorSystem: ActorSystem, config: Config): Source[(String, ServiceEvent), NotUsed] = {
    implicit val system: ActorSystem = actorSystem
    val settings                     = Settings.from(config)

    logger.debug("Initialising webhook provider")
    val name = classOf[WebhookProvider].getCanonicalName

    val specificationSource = if (settings.selfSpecification) {
      val specification = scala.io.Source.fromResource("webhook-specification.yaml").getLines().mkString("\n")
      Source.single(
        ServiceEvent.ServiceUp(
          Service("restui:webhook", "Webhook provider", specification, Map(Metadata.File -> "webhook-specification.yaml"))))
    } else Source.empty[ServiceEvent]

    Source
      .futureSource(HttpServer.start(settings.interface, settings.port))
      .mapMaterializedValue(_ => NotUsed)
      .prepend(specificationSource)
      .map(name -> _)
  }

}
