package restui.servicediscovery.git.github

import scala.collection.mutable
import scala.concurrent.Future
import scala.concurrent.duration._

import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpRequest, HttpResponse}
import akka.stream.scaladsl.Sink
import akka.testkit.TestProbe
import base.TestBase
import io.circe.syntax._
import org.scalatest.{Inside, Inspectors}
import restui.servicediscovery.git.git.data.{Repository => GitRepository}
import restui.servicediscovery.git.github.data.{Node, Repository}
import restui.servicediscovery.git.settings.{GitHub => GitHubSetting, Repository => RepositorySetting, Uri}

class GithubSpec extends TestBase with Inside with Inspectors {
  private val nodes = Seq(Node("MyAwesomeUser/MyAwesomeRepo", "https://github.com/MyAwesomeUser/MyAwesomeRepo", "master"))

  private val executor = (_: HttpRequest) =>
    Future.successful(HttpResponse(entity = HttpEntity(ContentTypes.`application/json`, Repository(nodes, None).asJson.noSpaces)))

  "Retrieving repositories in regular interval" in {
    val github =
      GitHubSetting("MyAwesomeUser",
                    "MyAwesomeUser",
                    pollingInterval = 800.millis,
                    repos = RepositorySetting(Uri("MyAwesomeUser/MyAwesomeRepo")) :: Nil)
    val client = GithubClient(github, executor)

    val probe = TestProbe()

    Github.retrieveRepositoriesRegularly(client).to(Sink.actorRef(probe.ref, "completed", _ => ())).run()
    val results = mutable.ListBuffer.empty[GitRepository]
    results += probe.expectMsgType[GitRepository](1.second)
    probe.expectNoMessage(200.millis)
    results += probe.expectMsgType[GitRepository](1.second)

    forAll(results) { result =>
      inside(result) {
        case GitRepository(uri, branch, _, swagger, ref) =>
          uri shouldBe "https://MyAwesomeUser@github.com/MyAwesomeUser/MyAwesomeRepo"
          branch shouldBe "master"
          swagger shouldBe empty
          ref should not be 'defined
      }
    }
  }
}
