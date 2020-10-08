package restui.providers.git.github

import scala.collection.mutable
import scala.concurrent.Future
import scala.concurrent.duration._

import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpRequest, HttpResponse}
import akka.stream.scaladsl.Sink
import base.TestBase
import io.circe.syntax._
import org.scalatest.{Inside, Inspectors}
import restui.providers.git.git.data.{Repository => GitRepository}
import restui.providers.git.github.data.{Node, Repository}
import restui.providers.git.settings.{GithubSettings, Location, RepositorySettings}

class GithubSpec extends TestBase with Inside with Inspectors {
  private val nodes = Seq(Node("MyAwesomeUser/MyAwesomeRepo", "https://github.com/MyAwesomeUser/MyAwesomeRepo", "master"))

  private val executor = (_: HttpRequest) =>
    Future.successful(HttpResponse(entity = HttpEntity(ContentTypes.`application/json`, Repository(nodes, None).asJson.noSpaces)))

  "Retrieving repositories in regular interval" in {
    val github =
      GithubSettings("MyAwesomeUser",
                     "MyAwesomeUser",
                     pollingInterval = 800.millis,
                     repos = RepositorySettings(Location.Uri("MyAwesomeUser/MyAwesomeRepo"), useProxy = false) :: Nil)
    val client = GithubClient(github, executor)

    val probe = testKit.createTestProbe[GitRepository]()

    Github(client).to(Sink.foreach(e => probe.ref ! e)).run()
    val results = mutable.ListBuffer.empty[GitRepository]
    results += probe.expectMessageType[GitRepository](1.second)
    probe.expectNoMessage(200.millis)
    probe.expectNoMessage(1.second)

    forAll(results) { result =>
      inside(result) {
        case GitRepository(uri, branch, _, specification, ref, useProxy) =>
          uri shouldBe "https://MyAwesomeUser@github.com/MyAwesomeUser/MyAwesomeRepo"
          branch shouldBe "master"
          specification shouldBe empty
          useProxy shouldBe false
          ref should not be Symbol("defined")
      }
    }
  }
}
