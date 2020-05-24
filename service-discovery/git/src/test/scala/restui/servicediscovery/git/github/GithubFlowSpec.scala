package restui.servicediscovery.git.github

import scala.concurrent.Future

import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpRequest, HttpResponse}
import akka.stream.scaladsl.Sink
import akka.testkit.TestProbe
import base.TestBase
import io.circe.syntax._
import org.scalatest.Inside
import restui.servicediscovery.git.git.{Repo => GitRepo}
import restui.servicediscovery.git.github.models.{Node, Repository}
import restui.servicediscovery.git.settings.{GitHub, Repo, Uri}
import scala.concurrent.duration._
import scala.collection.mutable
import org.scalatest.Inspectors

class GithubFlowSpec extends TestBase with Inside with Inspectors {
  private val nodes = Seq(Node("MyAwesomeUser/MyAwesomeRepo", "https://github.com/MyAwesomeUser/MyAwesomeRepo", "master"))

  private val executor = (_: HttpRequest) =>
    Future.successful(HttpResponse(entity = HttpEntity(ContentTypes.`application/json`, Repository(nodes, None).asJson.noSpaces)))

  "Retrieving repositories in regular interval" in {
    val github =
      GitHub("MyAwesomeUser", "MyAwesomeUser", pollingInterval = 800.millis, repos = Repo(Uri("MyAwesomeUser/MyAwesomeRepo")) :: Nil)
    val client = GithubClient(github, executor)

    val probe = TestProbe()

    GithubFlow.retrieveRepositoriesRegularly(client).to(Sink.actorRef(probe.ref, "completed", _ => ())).run()
    val results = mutable.ListBuffer.empty[GitRepo]
    results += probe.expectMsgType[GitRepo](1.second)
    probe.expectNoMessage(200.millis)
    results += probe.expectMsgType[GitRepo](1.second)

    forAll(results) { result =>
      inside(result) {
        case restui.servicediscovery.git.git.Repo(uri, branch, _, swagger, ref) =>
          uri shouldBe "https://MyAwesomeUser@github.com/MyAwesomeUser/MyAwesomeRepo"
          branch shouldBe "master"
          swagger shouldBe empty
          ref should not be 'defined
      }
    }
  }
}
