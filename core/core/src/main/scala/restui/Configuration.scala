package restui

import com.typesafe.config.{Config, ConfigFactory}
import org.slf4j.LoggerFactory
import scala.util.Try

import java.io.File

object Configuration {
  private val logger = LoggerFactory.getLogger(Configuration.getClass)

  private val referenceConfig: Config = ConfigFactory.defaultReference()

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
