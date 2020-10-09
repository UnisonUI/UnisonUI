package restui.providers.webook

import java.io.File
import java.nio.file.Path

import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model.{
  HttpMethods,
  HttpRequest,
  RequestEntity,
  StatusCodes,
  Uri
}
import akka.stream.scaladsl.Sink
import cats.syntax.either._
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport
import io.circe.generic.auto._
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AsyncWordSpecLike
import restui.models.{Metadata, Service, ServiceEvent}
import restui.protobuf.ProtobufCompiler
import restui.providers.webhook.HttpServer
import restui.providers.webhook.models.{Service => WebhookService}

class HttpServerSpec
    extends ScalaTestWithActorTestKit
    with AsyncWordSpecLike
    with Matchers
    with FailFastCirceSupport {
  implicit val compiler: ProtobufCompiler = new ProtobufCompiler {
    override def compile(path: Path): Either[Throwable, File] =
      new File(s"${path.toAbsolutePath().toString}set").asRight[Throwable]
    override def clean(file: File): Either[Throwable, Unit] = ().asRight
  }
  "Sending a service to the webhook server" should {
    "receive a no content response" in {
      val probe = testKit.createTestProbe[ServiceEvent]()
      val body  = WebhookService.OpenApi("test", "content")
      val expectedService = Service.OpenApi("webhook:test",
                                            "test",
                                            "content",
                                            Map(Metadata.Provider -> "webhook",
                                                Metadata.File     -> "test"))
      for {
        source <- HttpServer.start("localhost", 3000)
        _ = source.to(Sink.foreach(e => probe.ref ! e)).run()
        entity <- Marshal(body).to[RequestEntity]
        response <- Http().singleRequest(
          HttpRequest(method = HttpMethods.POST,
                      uri = Uri("http://localhost:3000/services"),
                      entity = entity))
      } yield {
        probe.expectMessage(ServiceEvent.ServiceUp(expectedService))
        response.status shouldBe StatusCodes.NoContent
      }
    }
  }
}
