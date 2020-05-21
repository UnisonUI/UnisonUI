package restui.servicediscovery.docker

import java.{util => ju}

import scala.jdk.CollectionConverters._

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import com.github.dockerjava.api.command.ListContainersCmd
import com.github.dockerjava.api.model.{Container, ContainerNetwork, ContainerNetworkSettings, Event}
import com.github.dockerjava.api.{DockerClient => JDockerClient}
import org.scalamock.scalatest.MockFactory
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import restui.servicediscovery.models._
import restui.stubs.EventsCmdStub

class DockerClientSpec
    extends TestKit(ActorSystem("test"))
    with ImplicitSender
    with AnyWordSpecLike
    with Matchers
    with MockFactory
    with BeforeAndAfterAll {

  override def beforeAll =
    Http().bindAndHandle(path(PathEnd)(complete(StatusCodes.OK)), "localhost", 9999)

  override def afterAll: Unit =
    TestKit.shutdownActorSystem(system)

  private val settings                   = Settings("myDocker.sock", Labels("name", "port", "swagger"))
  private val MatchingContainerLabels    = Map("name" -> "test", "port" -> "9999", "swagger" -> "/").asJava
  private val NonMatchingContainerLabels = Map("name" -> "test").asJava

  "Listing endpoints" when {
    "getting the running container" should {
      "find it" in {
        val clientMock = setupMockWithoutEvent(MatchingContainerLabels)
        val probe      = TestProbe()
        new DockerClient(clientMock, settings, event => probe.ref ! event).listCurrentAndFutureEndpoints
        probe.expectMsg(ServiceUp(Service("test", OpenApiFile(ContentTypes.Plain, "OK"))))
      }
      "not find it" when {
        "there is a missing label" in {
          val clientMock = setupMockWithoutEvent(NonMatchingContainerLabels)
          val probe      = TestProbe()
          new DockerClient(clientMock, settings, event => probe.ref ! event).listCurrentAndFutureEndpoints
          probe.expectNoMessage()
        }

        "there is no labels at all" in {
          val clientMock = setupMockWithoutEvent(Map.empty.asJava)
          val probe      = TestProbe()
          new DockerClient(clientMock, settings, event => probe.ref ! event).listCurrentAndFutureEndpoints
          probe.expectNoMessage()
        }
      }
    }
  }

  private def setupMockWithoutEvent(labels: ju.Map[String, String]) = setupMock(None, labels)

  private def setupMock(maybeEvent: Option[Event], labels: ju.Map[String, String]) = {
    val clientMock               = mock[JDockerClient]
    val listContainersCmd        = mock[ListContainersCmd]
    val containerNetworkSettings = mock[ContainerNetworkSettings]
    val containerNetwork         = mock[ContainerNetwork]
    val eventsCmd                = new EventsCmdStub(maybeEvent)
    val container                = mock[Container]
    (clientMock.listContainersCmd _).expects() returning listContainersCmd
    (clientMock.eventsCmd _).expects() returning eventsCmd

    (listContainersCmd.withStatusFilter _) expects (Seq("running").asJavaCollection) returning listContainersCmd

    (listContainersCmd.exec _).expects() returning List(container).asJava

    (container.getLabels _).expects() returning labels
    (container.getNetworkSettings _).expects() returning containerNetworkSettings
    (containerNetworkSettings.getNetworks _).expects() returning Map("test" -> containerNetwork).asJava
    (containerNetwork.getIpAddress _).expects().returning("127.0.0.1").anyNumberOfTimes
    clientMock

  }
}
