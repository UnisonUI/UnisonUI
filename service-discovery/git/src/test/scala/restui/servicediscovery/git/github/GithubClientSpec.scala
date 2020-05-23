package restui.servicediscovery.git.github

import scala.concurrent.Future

import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpResponse}
import akka.stream.scaladsl.Sink
import base.TestBase
import io.circe.syntax._
import org.scalatest.EitherValues
import restui.Concurrency
import restui.servicediscovery.git.github.models.{Node, Repository}
import restui.servicediscovery.git.settings.GitHub

class GithubClientSpec extends TestBase with EitherValues {
  "Executing an existing command" in {
    println(Concurrency.AvailableCore)
    val github = GitHub("MyAwesomeUser", "MyAwesomeUser")
    val nodes  = Seq(Node("MyAwesomeUser/MyAwesomeRepo", "https://github.com/MyAwesomeUser/MyAwesomeRepo"))
    val client = GithubClient(
      github,
      _ => Future.successful(HttpResponse(entity = HttpEntity(ContentTypes.`application/json`, Repository(nodes, None).asJson.noSpaces)))
    )
    GithubClient.listRepositories(client).runWith(Sink.seq).map { result =>
      result should contain theSameElementsAs nodes
    }
  }
}
