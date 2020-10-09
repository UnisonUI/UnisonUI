package restui

import java.io.File

import com.typesafe.config.{Config, ConfigFactory}
import com.typesafe.scalalogging.LazyLogging

import scala.util.Try

object Configuration extends LazyLogging {
  private val defaultConfig: Config = ConfigFactory.load()

  def config(maybeFile: Option[String] = None): Config =
    maybeFile.flatMap { filename =>
      Try {
        val file = new File(filename)
        ConfigFactory.systemProperties
          .withFallback(ConfigFactory.parseFile(file))
          .withFallback(defaultConfig)
      }.recover { case throwable =>
        logger.warn(
          "Failed to load the default configuration, attempting to load the reference configuration",
          throwable)
        defaultConfig
      }.toOption
    }.getOrElse(defaultConfig).resolve
}
