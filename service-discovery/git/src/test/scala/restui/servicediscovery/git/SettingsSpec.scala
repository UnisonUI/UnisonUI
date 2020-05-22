package restui.servicediscovery.git

import scala.concurrent.duration._

import com.typesafe.config.ConfigFactory
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import restui.servicediscovery.git.settings._

class SettingsSpec extends AnyWordSpec with Matchers {
  "Settings" should {

    "not load any vcs" when {

      "the entry is empty" in {
        val config = ConfigFactory.parseString("""restui.service-discovery.git {
|  cache-duration =  "2 hours"
|  vcs {
|  }
|}""".stripMargin)
        Settings.from(config) shouldBe Settings(2.hours, Nil)
      }

      "the entry is missing" in {
        val config = ConfigFactory.parseString("""restui.service-discovery.git {
|  cache-duration =  "2 hours"
|}""".stripMargin)
        Settings.from(config) shouldBe Settings(2.hours, Nil)
      }

    }

    "load github" when {
      "there is no repos" in {
        val config = ConfigFactory.parseString("""restui.service-discovery.git {
|  cache-duration =  "2 hours"
|  vcs {
|    github {
|     api-token = "test" 
|     polling-interval = "10 minutes"
|    }
|  }
|}""".stripMargin)
        Settings.from(config) shouldBe Settings(2.hours, List(GitHub("test", "https://api.github.com", 10.minutes, Nil)))
      }

      "there is repos" in {
        val config = ConfigFactory.parseString("""restui.service-discovery.git {
|  cache-duration =  "2 hours"
|  vcs {
|    github {
|     api-token = "test"
|     repos = [
|       {
|         location = "myOrg/Test"
|         swagger-paths = ["test/"]
|       },
|       {
|         location = "/myOrg\/.+/"
|       },
|       "restui"
|     ]
|    }
|  }
|}""".stripMargin)
        Settings.from(config) shouldBe Settings(
          2.hours,
          List(
            GitHub("test",
                   "https://api.github.com",
                   1.hours,
                   Repo(Uri("myOrg/Test"), List("test/")) :: Repo(Regex("myOrg/.+"), Nil) :: Repo(Uri("restui"), Nil) :: Nil)
          )
        )
      }
    }

    "load git" in {
      val config = ConfigFactory.parseString("""restui.service-discovery.git {
|  cache-duration =  "2 hours"
|  vcs {
|    git {
|     repos = [
|       {
|         location = "myOrg/Test"
|         swagger-paths = ["test/"]
|       },
|       {
|         location = "/myOrg\/.+/"
|       },
|       "restui"
|     ]
|    }
|  }
|}""".stripMargin)
      Settings.from(config) shouldBe Settings(
        2.hours,
        List(
          Git(Repo(Uri("myOrg/Test"), List("test/")) :: Repo(Regex("myOrg/.+"), Nil) :: Repo(Uri("restui"), Nil) :: Nil)
        )
      )
    }
  }

}
