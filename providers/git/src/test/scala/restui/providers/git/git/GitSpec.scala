package restui.providers.git.git

import java.nio.file.Files

import scala.concurrent.duration._

import akka.stream.scaladsl.{Sink, Source}
import akka.testkit.TestProbe
import base.TestBase
import org.scalatest.Inside
import restui.models.{Service, ServiceEvent}
import restui.providers.git.git.data._
class GitSpec extends TestBase with Inside {

  private val duration = 50.millis
  trait StubRepository {
    import sys.process._
    val repo       = Files.createTempDirectory("restui-git-test")
    val hideStdErr = ProcessLogger(_ => ())

    Process(Seq("git", "init"), repo.toFile()).!(hideStdErr) shouldBe 0
    commit("init", "init")

    def commit(path: String, text: String): Unit = {
      val file = Files.write(repo.resolve(path), text.getBytes())
      Process(Seq("git", "add", file.toAbsolutePath.toString), repo.toFile()).!(hideStdErr)
      Process(Seq("git", "commit", "-m", "new file"), repo.toFile()).!(hideStdErr)
    }

    def sha1(branch: String): String =
      Process(Seq("git", "rev-parse", "--verify", branch), repo.toFile()).!!(hideStdErr).trim

  }

  "Executing a git flow" when {
    "there is a failure with a git command" in {
      val fixture = new StubRepository {}
      val tempDir = Files.createTempDirectory("restui-git-test-clone").toFile
      val repo    = Repository(fixture.repo.toAbsolutePath.toString, "i-do-not-exists", List(UnnamedSpecification("test")), Some(tempDir))

      val probe = TestProbe()
      Git.fromSource(duration, Source.single(repo)).to(Sink.actorRef(probe.ref, "completed", _ => ())).run()
      probe.expectNoMessage()
    }

    "retrieving files from git" should {

      "not find files" when {

        "there is no matching files" in {
          val fixture = new StubRepository {}
          val tempDir = Files.createTempDirectory("restui-git-test-clone").toFile
          val repo    = Repository(fixture.repo.toAbsolutePath.toString, "master", List(UnnamedSpecification("test")), Some(tempDir))

          val probe = TestProbe()
          Git.fromSource(duration, Source.single(repo)).to(Sink.actorRef(probe.ref, "completed", _ => ())).run()
          probe.expectNoMessage()
        }
      }

      "find files" when {
        "no new file is present" in {
          val fixture = new StubRepository {}
          fixture.commit("test", "test")
          val tempDir = Files.createTempDirectory("restui-git-test-clone").toFile
          val repo    = Repository(fixture.repo.toAbsolutePath.toString, "master", List(UnnamedSpecification("test")), Some(tempDir))

          val probe = TestProbe()
          Git.fromSource(duration, Source.single(repo)).to(Sink.actorRef(probe.ref, "completed", _ => ())).run()
          val result = probe.expectMsgType[ServiceEvent]
          inside(result) {
            case ServiceEvent.ServiceUp(Service(_, _, file, _, _)) =>
              file shouldBe "test"
          }
        }
        "a new file is present" in {
          val fixture = new StubRepository {}
          fixture.commit("test", "test")
          val tempDir = Files.createTempDirectory("restui-git-test-clone").toFile
          val repo    = Repository(fixture.repo.toAbsolutePath.toString, "master", List(UnnamedSpecification("test")), Some(tempDir))

          val probe = TestProbe()
          Git.fromSource(duration, Source.single(repo)).to(Sink.actorRef(probe.ref, "completed", _ => ())).run()

          inside(probe.expectMsgType[ServiceEvent]) {
            case ServiceEvent.ServiceUp(Service(_, _, file, _, _)) =>
              file shouldBe "test"
          }

          fixture.commit("test", "test2")

          inside(probe.expectMsgType[ServiceEvent]) {
            case ServiceEvent.ServiceUp(Service(_, _, file, _, _)) =>
              file shouldBe "test2"
          }

        }
      }

    }
  }

}
