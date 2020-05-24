package restui

import java.io.File

import scala.util.Try

import com.typesafe.config.{Config, ConfigFactory}
import com.typesafe.scalalogging.LazyLogging

object Configuration extends LazyLogging {
  private val defaultConfig: Config = ConfigFactory.load()
  def config(maybeFile: Option[String] = None): Config =
    maybeFile.flatMap {
      case filename =>
        Try {
          val file = new File(filename)
          ConfigFactory.parseFile(file).withFallback(defaultConfig)
        }.recover {
          case throwable =>
            logger.warn("Failed to load the default configuration, attempting to load the reference configuration", throwable)
            defaultConfig
        }.toOption
    }.getOrElse(defaultConfig)
}
