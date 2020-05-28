package restui.providers.git.git

import java.nio.file.Files

import akka.stream.scaladsl.{Sink, Source}
import base.TestBase
import org.scalatest.Inside
import restui.providers.git.git.data.Repository
import restui.models.{ContentTypes, OpenApiFile, Service}

class GitSpec extends TestBase with Inside {

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
      val repo    = Repository(fixture.repo.toAbsolutePath.toString, "i-do-not-exists", List("test"), Some(tempDir))

      Source.single(repo).via(Git.flow).runWith(Sink.seq).map { result =>
        result shouldBe empty
      }
    }

    "retrieving files from git" should {

      "not find files" when {

        "there is no matching files" in {
          val fixture = new StubRepository {}
          val tempDir = Files.createTempDirectory("restui-git-test-clone").toFile
          val repo    = Repository(fixture.repo.toAbsolutePath.toString, "master", List("test"), Some(tempDir))

          Source.single(repo).via(Git.flow).runWith(Sink.seq).map { result =>
            result shouldBe empty
          }
        }
      }

      "find files" in {
        val fixture = new StubRepository {}
        fixture.commit("test", "test")
        val tempDir = Files.createTempDirectory("restui-git-test-clone").toFile
        val repo    = Repository(fixture.repo.toAbsolutePath.toString, "master", List("test"), Some(tempDir))

        Source.single(repo).via(Git.flow).runWith(Sink.seq).map { result =>
          result should have length 1
          inside(result.head) {
            case Service(_, file, _) =>
              file shouldBe OpenApiFile(ContentTypes.Plain, "test")
          }
        }
      }
    }
  }

}
