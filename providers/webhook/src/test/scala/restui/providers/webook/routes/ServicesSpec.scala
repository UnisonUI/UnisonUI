package restui.providers.webook.routes

import java.io.File
import java.nio.file.Path

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.stream.OverflowStrategy
import akka.stream.scaladsl.{Keep, Sink, Source, SourceQueueWithComplete}
import akka.testkit.{TestKit, TestProbe}
import cats.syntax.either._
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport
import io.circe.generic.auto._
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import restui.models.{Metadata, Service, ServiceEvent}
import restui.protobuf.ProtobufCompiler
import restui.providers.webhook.models.{Service => WebhookService}
import restui.providers.webhook.routes.Services

class ServicesSpec extends AnyWordSpec with ScalatestRouteTest with Matchers with BeforeAndAfterAll with FailFastCirceSupport {
  implicit val compiler: ProtobufCompiler = new ProtobufCompiler {
    override def compile(path: Path): Either[Throwable, File] = new File(s"${path.toAbsolutePath().toString}set").asRight[Throwable]
    override def clean(file: File): Either[Throwable, Unit]   = ().asRight
  }
  override def afterAll(): Unit =
    TestKit.shutdownActorSystem(system)

  private def createQueue: (SourceQueueWithComplete[ServiceEvent], TestProbe) = {
    val probe = TestProbe()
    val queue =
      Source.queue[ServiceEvent](0, OverflowStrategy.backpressure).toMat(Sink.actorRef(probe.ref, "completed", _ => ()))(Keep.left).run()
    queue -> probe
  }

  "Creating a service" when {
    "the content send is valid" should {
      "return no content" in {
        val (queue, probe) = createQueue
        val body           = WebhookService.OpenApi("test", "content")
        val expectedService =
          Service.OpenApi("webhook:test", "test", "content", Map(Metadata.Provider -> "webhook", Metadata.File -> "test"))
        Post("/services", body) ~> Services.route(queue) ~> check {
          response.status shouldBe StatusCodes.NoContent
          probe.expectMsg(ServiceEvent.ServiceUp(expectedService))
        }
      }
    }
  }

  "Deleting a service" should {
    "return no content" in {
      val (queue, probe) = createQueue

      Delete("/services/test") ~> Services.route(queue) ~> check {
        response.status shouldBe StatusCodes.NoContent
        probe.expectMsg(ServiceEvent.ServiceDown("webhook:test"))
      }
    }
  }
}
