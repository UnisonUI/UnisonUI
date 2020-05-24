package restui.servicediscovery.git.github

import scala.concurrent.Future

import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpResponse}
import akka.stream.scaladsl.Sink
import base.TestBase
import io.circe.syntax._
import restui.servicediscovery.git.github.models.{Error, Node, Repository}
import restui.servicediscovery.git.settings.GitHub

class GithubClientSpec extends TestBase {
  private val github = GitHub("MyAwesomeUser", "MyAwesomeUser")
  private val nodes  = Seq(Node("MyAwesomeUser/MyAwesomeRepo", "https://github.com/MyAwesomeUser/MyAwesomeRepo", "master"))

  "Listing repositories" when {

    "there is no pagination" in {
      val client = GithubClient(
        github,
        _ => Future.successful(HttpResponse(entity = HttpEntity(ContentTypes.`application/json`, Repository(nodes, None).asJson.noSpaces)))
      )

      GithubClient.listRepositories(client).runWith(Sink.seq).map { result =>
        result should contain theSameElementsAs nodes
      }
    }

    "there is pagination" in {
      var ref: Option[String] = Some("ref")
      val client = GithubClient(
        github,
        _ => {
          val json = ref match {
            case None => Repository(nodes, None).asJson.noSpaces
            case Some(value) =>
              ref = None
              Repository(nodes, Some(value)).asJson.noSpaces
          }

          Future.successful(HttpResponse(entity = HttpEntity(ContentTypes.`application/json`, json)))
        }
      )

      GithubClient.listRepositories(client).runWith(Sink.seq).map { result =>
        result should contain theSameElementsAs nodes ++ nodes
      }
    }

    "there is an error" when {

      "it's from the api response" in {
        val client = GithubClient(
          github,
          _ =>
            Future.successful(HttpResponse(entity = HttpEntity(ContentTypes.`application/json`, Error(List("Some error")).asJson.noSpaces)))
        )

        GithubClient.listRepositories(client).runWith(Sink.seq).map { result =>
          result shouldBe empty
        }
      }
      "it's from the connection" in {
        val client = GithubClient(
          github,
          _ => Future.failed(new Exception("some exception"))
        )

        GithubClient.listRepositories(client).runWith(Sink.seq).map { result =>
          result shouldBe empty
        }
      }
    }
  }
}
