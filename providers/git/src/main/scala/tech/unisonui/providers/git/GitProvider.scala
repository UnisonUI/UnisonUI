package tech.unisonui.providers.git

import akka.NotUsed
import akka.actor.typed.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.scaladsl.Source
import com.typesafe.config.Config
import com.typesafe.scalalogging.LazyLogging
import tech.unisonui.models.ServiceEvent
import tech.unisonui.protobuf.{ProtobufCompiler, ProtobufCompilerImpl}
import tech.unisonui.providers.Provider
import tech.unisonui.providers.git.settings.Settings
import tech.unisonui.providers.git.vcs.VCS

import scala.concurrent.ExecutionContext

// $COVERAGE-OFF$
class GitProvider extends Provider with LazyLogging {
  override def start(
      actorSystem: ActorSystem[_],
      config: Config): Source[(String, ServiceEvent), NotUsed] = {
    val name                            = classOf[GitProvider].getCanonicalName
    implicit val system: ActorSystem[_] = actorSystem
    implicit val executionContext: ExecutionContext =
      actorSystem.executionContext
    implicit val protobufCompiler: ProtobufCompiler = new ProtobufCompilerImpl
    val settings                                    = Settings.from(config)

    logger.debug("Initialising git provider")

    VCS
      .source(settings, Http().singleRequest(_))
      .mapMaterializedValue(_ => NotUsed)
      .map(event => name -> event)
  }

}
