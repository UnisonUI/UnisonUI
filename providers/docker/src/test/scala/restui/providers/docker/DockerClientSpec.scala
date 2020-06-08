package restui.providers.docker

import scala.concurrent.{ExecutionContext, Future}

import akka.actor.ActorSystem
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpResponse, StatusCodes}
import akka.stream.scaladsl.{Sink, Source}
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import io.circe.syntax._
import org.scalamock.scalatest.MockFactory
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import restui.models.{Metadata, Service, ServiceEvent}
import restui.providers.docker.client.HttpClient
import restui.providers.docker.client.models.{Container, Event, State}

class DockerClientSpec
    extends TestKit(ActorSystem("test"))
    with ImplicitSender
    with AnyWordSpecLike
    with Matchers
    with MockFactory
    with BeforeAndAfterAll {
  implicit val ec: ExecutionContext = system.dispatcher
  private val Id                    = "12345"
  private val ServiceName           = "test"

  override def afterAll: Unit =
    TestKit.shutdownActorSystem(system)

  private val settings                   = Settings("myDocker.sock", Labels("name", "port", "specification"))
  private val MatchingContainerLabels    = Map("name" -> ServiceName, "port" -> "9999", "specification" -> "/openapi.yaml")
  private val NonMatchingContainerLabels = Map("name" -> ServiceName)

  "Streaming the containers" when {
    "there is an error" when {

      "the events endpoint fail" in {
        val clientMock = mock[HttpClient]
        (clientMock.watch _)
          .expects(*)
          .returning(Source.single(HttpResponse(status = StatusCodes.BadRequest)))

        val probe = TestProbe()
        new DockerClient(clientMock, settings).startStreaming.to(Sink.actorRef(probe.ref, "completed", _ => ())).run()

        probe.expectMsg("completed")

      }

      "could not decode event" in {
        val clientMock = mock[HttpClient]
        (clientMock.watch _)
          .expects(*)
          .returning(
            Source.single(
              HttpResponse(
                entity = HttpEntity(ContentTypes.`application/json`, "{}")
              )))

        val probe = TestProbe()
        new DockerClient(clientMock, settings).startStreaming.to(Sink.actorRef(probe.ref, "completed", _ => ())).run()

        probe.expectMsg("completed")

      }

      "the container endpoint fail" in {
        val clientMock = mock[HttpClient]
        (clientMock.watch _)
          .expects(*)
          .returning(
            Source.single(
              HttpResponse(
                entity = HttpEntity(ContentTypes.`application/json`, Event(Id, Some(State.Start), MatchingContainerLabels).asJson.noSpaces)
              )))

        (clientMock.get _)
          .expects(*)
          .returning(
            Future.successful(
              HttpResponse(status = StatusCodes.BadRequest)
            )
          )
        val probe = TestProbe()
        new DockerClient(clientMock, settings).startStreaming.to(Sink.actorRef(probe.ref, "completed", _ => ())).run()

        probe.expectMsg("completed")

      }
    }

    "there is a container which is up and one down" should {
      "find it" in {
        val clientMock =
          setupMock(MatchingContainerLabels,
                    List(
                      Event(Id, Some(State.Start), MatchingContainerLabels),
                      Event(Id, Some(State.Stop), MatchingContainerLabels)
                    ))
        val probe = TestProbe()
        new DockerClient(clientMock, settings).startStreaming.to(Sink.actorRef(probe.ref, "completed", _ => ())).run()

        probe.expectMsg(ServiceEvent.ServiceDown(Id))
        probe.expectMsg(
          ServiceEvent.ServiceUp(
            Service(Id, ServiceName, "OK", Map(Metadata.Provider -> "docker", Metadata.File -> "openapi.yaml"))
          )
        )
      }

      "not find it" when {
        "there is a missing label" in {
          val clientMock = setupMock(NonMatchingContainerLabels, List(Event(Id, Some(State.Start), NonMatchingContainerLabels)))
          val probe      = TestProbe()
          new DockerClient(clientMock, settings).startStreaming.to(Sink.actorRef(probe.ref, "completed", _ => ())).run()
          probe.expectMsg("completed")
        }

        "there is no labels at all" in {
          val clientMock = setupMock(Map.empty, Nil)
          val probe      = TestProbe()
          new DockerClient(clientMock, settings).startStreaming.to(Sink.actorRef(probe.ref, "completed", _ => ())).run()
          probe.expectMsg("completed")
        }
      }
    }
  }

  private def setupMock(labels: Map[String, String], events: List[Event]) = {
    val clientMock = mock[HttpClient]

    (clientMock.watch _)
      .expects(*)
      .returning(Source(events.map { event =>
        HttpResponse(
          entity = HttpEntity(ContentTypes.`application/json`, event.asJson.noSpaces)
        )
      }))
      .once

    (clientMock.get _)
      .expects(*)
      .returning(
        Future.successful(
          HttpResponse(
            entity = HttpEntity(ContentTypes.`application/json`, Container(labels, Some("localhost")).asJson.noSpaces)
          )))
      .anyNumberOfTimes

    (clientMock.downloadFile _).expects("http://localhost:9999/openapi.yaml").returning(Future.successful("OK")).anyNumberOfTimes

    clientMock

  }
}
