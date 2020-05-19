package restui

import java.io.File

import scala.util.Try

import com.typesafe.config.{Config, ConfigFactory}
import org.slf4j.LoggerFactory

object Configuration {
  private val logger = LoggerFactory.getLogger(Configuration.getClass)

  private val referenceConfig: Config = ConfigFactory.load()
  val config: Config = Try {
    val file = new File("application.conf")
    ConfigFactory.parseFile(file).withFallback(referenceConfig)
  }.fold(
    throwable => {
      logger.warn("Failed to load the default configuration, attempting to load the reference configuration", throwable)
      referenceConfig
    },
    identity(_)
  )
}
