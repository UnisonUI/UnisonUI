package restui

import com.typesafe.config.{Config, ConfigFactory}
import org.scalatest.compatible.Assertion
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class ConfigurationSpec extends AnyWordSpec with Matchers {
  private def checkConfiguration(config: Config,
                                 value: String = "configuration"): Assertion =
    config.getString("test") shouldBe value

  "Getting the configuration" when {
    "not providing an external file" in {
      checkConfiguration(Configuration.config(None))
    }
    "providing an external configuration" should {
      "fallback to default configuration" when {
        "the file does not exist" in {
          checkConfiguration(
            Configuration.config(Some("not-existing-file.conf")))
        }
        "the file is not a valid HOCON file" in {
          checkConfiguration(
            Configuration.config(Some("src/test/resources/invalid.conf")))
        }
      }
      "get the value from the file" in {
        checkConfiguration(
          Configuration.config(Some("src/test/resources/valid.conf")),
          "valid")
      }
      "get the value from the system property over the file" in {
        System.setProperty("test", "property")
        ConfigFactory.invalidateCaches()
        checkConfiguration(
          Configuration.config(Some("src/test/resources/valid.conf")),
          "property")
      }
    }
  }
}
