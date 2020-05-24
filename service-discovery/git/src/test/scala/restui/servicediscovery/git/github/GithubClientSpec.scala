package restui.servicediscovery.git.github

import scala.concurrent.Future

import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpResponse}
import akka.stream.scaladsl.Sink
import base.TestBase
import io.circe.syntax._
import restui.servicediscovery.git.github.models.{Node, Repository}
import restui.servicediscovery.git.settings.GitHub

class GithubClientSpec extends TestBase {
  "Executing an existing command" in {
    val github = GitHub("MyAwesomeUser", "MyAwesomeUser")
    val nodes  = Seq(Node("MyAwesomeUser/MyAwesomeRepo", "https://github.com/MyAwesomeUser/MyAwesomeRepo", "master"))
    val client = GithubClient(
      github,
      _ => Future.successful(HttpResponse(entity = HttpEntity(ContentTypes.`application/json`, Repository(nodes, None).asJson.noSpaces)))
    )

    GithubClient.listRepositories(client).runWith(Sink.seq).map { result =>
      result should contain theSameElementsAs nodes
    }
  }
}
