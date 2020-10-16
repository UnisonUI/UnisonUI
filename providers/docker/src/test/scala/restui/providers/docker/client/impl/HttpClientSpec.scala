package restui.providers.docker.client.impl

import java.nio.file.Files

import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives.{complete => httpComplete, _}
import akka.http.scaladsl.unmarshalling.Unmarshaller
import akka.stream.alpakka.unixdomainsocket.scaladsl.UnixDomainSocket
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.util.ByteString
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AsyncWordSpecLike

class HttpClientSpec
    extends ScalaTestWithActorTestKit
    with AsyncWordSpecLike
    with Matchers {
  private val httpResponse = ByteString("""HTTP/1.1 200 OK
|Content-Type: text/plain; charset=UTF-8
|Content-Length: 2
|Server: Test
|Accept-Ranges: bytes
|Connection: close
|
|OK
""".stripMargin)
  "Get a resource" when {
    "using an unix domain socket" in {
      val sockFile =
        Files.createTempFile("restui_unix", ".sock").toAbsolutePath
      sockFile.toFile.delete
      val client = new HttpClient(s"unix://${sockFile.toString}")
      for {
        server <- UnixDomainSocket().bindAndHandle(
          Flow[ByteString].map(_ => httpResponse),
          sockFile
        )
        response <- client.get(Uri("/"))
        body     <- Unmarshaller.stringUnmarshaller(response.entity)
        _        <- server.unbind()
      } yield {
        response.status shouldBe StatusCodes.OK
        body shouldBe "OK"
      }
    }
  }

  "Watching a resource" when {
    "there is no error" in {
      val sockFile =
        Files.createTempFile("restui_unix", ".sock").toAbsolutePath
      sockFile.toFile.delete
      val client = new HttpClient(s"unix://${sockFile.toString}")
      for {
        server <- UnixDomainSocket().bindAndHandle(
          Flow[ByteString].map(_ => httpResponse),
          sockFile
        )
        result <-
          client
            .watch(Uri("/"))
            .flatMapConcat { response =>
              response.status shouldBe StatusCodes.OK
              Source.future(Unmarshaller.stringUnmarshaller(response.entity))
            }
            .runWith(Sink.seq)
        _ <- server.unbind()
      } yield result shouldBe Seq("OK")
    }
    "there is an error" in {
      val client = new HttpClient("unix:///unknown")

      client
        .watch(Uri("/"))
        .map { response =>
          response.status shouldBe StatusCodes.OK
          Source.future(Unmarshaller.stringUnmarshaller(response.entity))
        }
        .runWith(Sink.seq)
        .map { result =>
          result shouldBe empty
        }
    }
  }
  "Downloading a file" in {
    val route  = path("file")(get(httpComplete(StatusCodes.OK)))
    val client = new HttpClient("unix:///unknown")
    for {
      server <- Http().newServerAt("localhost", 8080).bind(route)
      result <- client.downloadFile("http://localhost:8080/file")
      _      <- server.unbind()
    } yield result shouldBe "OK"
  }
}
