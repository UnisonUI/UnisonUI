package restui.providers.git

import scala.concurrent.duration._

import com.typesafe.config.{Config, ConfigFactory}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import restui.providers.git.settings._

class SettingsSpec extends AnyWordSpec with Matchers {
  private val defaultConfig                      = ConfigFactory.defaultReference()
  private def loadConfig(config: String): Config = ConfigFactory.parseString(config).withFallback(defaultConfig)
  private val repositories                       = """ repositories = [
|       {
|         location = "myOrg/Test"
|         specification-paths = ["test/"]
|       },
|       {
|         location = "/myOrg\/.+/"
|       },
|       "restui"
|     ]
|""".stripMargin

  "Settings" should {

    "not load any vcs" when {

      "the entry is empty" in {
        val config = loadConfig("""restui.provider.git {
|  cache-duration =  "2 hours"
|  vcs {
|  }
|}""".stripMargin)
        Settings.from(config) shouldBe Settings(2.hours, Nil)
      }

      "the entry is missing" in {
        val config = loadConfig("""restui.provider.git {
|  cache-duration =  "2 hours"
|}""".stripMargin)
        Settings.from(config) shouldBe Settings(2.hours, Nil)
      }

    }

    "load github" when {
      "there is no repos" in {
        val config = loadConfig("""restui.provider.git {
|  cache-duration =  "2 hours"
|  vcs {
|    github {
|     api-token = "test"
|     polling-interval = "10 minutes"
|    }
|  }
|}""".stripMargin)
        Settings.from(config) shouldBe Settings(2.hours, Nil)
      }

      "there is repos" in {
        val config = loadConfig(s"""restui.provider.git {
|  cache-duration =  "2 hours"
|  vcs {
|    github {
|     api-token = "test"
|     $repositories
|    }
|  }
|}""".stripMargin)
        Settings.from(config) shouldBe Settings(
          2.hours,
          List(
            GithubSettings(
              "test",
              "https://api.github.com/graphql",
              1.hours,
              RepositorySettings(Location.Uri("myOrg/Test"), None, List("test/")) :: RepositorySettings(
                Location.Regex("myOrg/.+"),
                None,
                Nil) :: RepositorySettings(Location.Uri("restui"), None, Nil) :: Nil
            )
          )
        )
      }
    }

    "load git" in {
      val config = loadConfig(s"""restui.provider.git {
|  cache-duration =  "2 hours"
|  vcs {
|    git {
|     $repositories
|    }
|  }
|}""".stripMargin)
      Settings.from(config) shouldBe Settings(
        2.hours,
        List(
          GitSettings(
            RepositorySettings(Location.Uri("myOrg/Test"), None, List("test/")) :: RepositorySettings(
              Location.Regex("myOrg/.+"),
              None,
              Nil) :: RepositorySettings(Location.Uri("restui"), None, Nil) :: Nil)
        )
      )
    }

  }

}
