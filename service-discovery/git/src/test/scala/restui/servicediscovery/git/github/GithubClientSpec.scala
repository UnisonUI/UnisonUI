package restui.servicediscovery.git.github

import scala.concurrent.Future

import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpResponse}
import akka.stream.scaladsl.Sink
import base.TestBase
import org.scalatest.EitherValues
import restui.Concurrency
import restui.servicediscovery.git.github.GithubClient.Repository
import restui.servicediscovery.git.settings.GitHub

class GithubClientSpec extends TestBase with EitherValues {
  "Executing an existing command" in {
    println(Concurrency.AvailableCore)
    val github = GitHub("MyAwesomeUser", "MyAwesomeUser")
    val client = GithubClient(
      github,
      _ =>
        Future.successful(
          HttpResponse(entity = HttpEntity(
            ContentTypes.`application/json`,
            """{
"data": {
  "viewer": {
    "repositories": {
      "pageInfo": {
        "endCursor": "myEndCursor",
        "hasNextPage": false
      },
      "nodes": [
        {
          "nameWithOwner": "MyAwesomeUser/MyAwesomeRepo",
          "projectsUrl": "https://github.com/MyAwesomeUser/MyAwesomeRepo"
        }
      ]
    }
  }
}
    }"""
          )))
    )
    GithubClient.listRepositories(client).runWith(Sink.seq).map { result =>
      result should contain theSameElementsAs Seq(
        Repository("MyAwesomeUser/MyAwesomeRepo", "https://github.com/MyAwesomeUser/MyAwesomeRepo"))
    }
  }
}
