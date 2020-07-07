package restui.providers.webook

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model.{HttpMethods, HttpRequest, RequestEntity, StatusCodes, Uri}
import akka.stream.scaladsl.Sink
import akka.testkit.{TestKit, TestProbe}
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport
import io.circe.generic.auto._
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AsyncWordSpecLike
import restui.models.{Metadata, Service, ServiceEvent}
import restui.providers.webhook.HttpServer
import restui.providers.webhook.models.{Service => WebhookService}

class HttpServerSpec
    extends TestKit(ActorSystem("test"))
    with AsyncWordSpecLike
    with Matchers
    with BeforeAndAfterAll
    with FailFastCirceSupport {

  override def afterAll(): Unit =
    TestKit.shutdownActorSystem(system)

  "Sending a service to the webhook server" should {
    "receive a no content response" in {
      val probe = TestProbe()

      val body            = WebhookService("test", "content")
      val expectedService = Service("webhook:test", "test", "content", Map(Metadata.Provider -> "webhook", Metadata.File -> "test"))
      for {
        source <- HttpServer.start("localhost", 3000)
        _ = source.to(Sink.actorRef(probe.ref, "completed", _ => ())).run()
        entity <- Marshal(body).to[RequestEntity]
        response <-
          Http().singleRequest(HttpRequest(method = HttpMethods.POST, uri = Uri("http://localhost:3000/services"), entity = entity))
      } yield {
        probe.expectMsg(ServiceEvent.ServiceUp(expectedService))
        response.status shouldBe StatusCodes.NoContent
      }
    }
  }
}
