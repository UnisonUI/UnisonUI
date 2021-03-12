package tech.unisonui.providers

import akka.NotUsed
import akka.actor.typed.ActorSystem
import akka.stream.scaladsl.Source
import com.typesafe.config.Config
import com.typesafe.scalalogging.LazyLogging
import tech.unisonui.models.{Metadata, Service, ServiceEvent}
import tech.unisonui.protobuf.{ProtobufCompiler, ProtobufCompilerImpl}
import tech.unisonui.providers.Provider
import tech.unisonui.providers.webhook.HttpServer
import tech.unisonui.providers.webhook.settings.Settings

// $COVERAGE-OFF$
class WebhookProvider extends Provider with LazyLogging {
  override def start(
      actorSystem: ActorSystem[_],
      config: Config): Source[(String, ServiceEvent), NotUsed] = {
    implicit val system: ActorSystem[_]             = actorSystem
    implicit val protobufCompiler: ProtobufCompiler = new ProtobufCompilerImpl
    val settings                                    = Settings.from(config)

    logger.debug("Initialising webhook provider")
    val name = classOf[WebhookProvider].getCanonicalName

    val specificationSource = if (settings.selfSpecification) {
      val specification = scala.io.Source
        .fromResource("webhook-specification.yaml")
        .getLines()
        .mkString("\n")
      Source.single(
        ServiceEvent.ServiceUp(
          Service.OpenApi("unisonui:webhook",
                          "Webhook provider",
                          specification,
                          Map(Metadata.File -> "webhook-specification.yaml"))))
    } else Source.empty[ServiceEvent]

    Source
      .futureSource(HttpServer.start(settings.interface, settings.port))
      .mapMaterializedValue(_ => NotUsed)
      .prepend(specificationSource)
      .map(name -> _)
  }

}
