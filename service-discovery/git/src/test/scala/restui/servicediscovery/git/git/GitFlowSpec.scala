package restui.servicediscovery.git.git

import java.nio.file.Files

import akka.stream.scaladsl.{Sink, Source}
import base.TestBase
import org.scalatest.OptionValues
import restui.servicediscovery.models.{ContentTypes, OpenApiFile}

class GitFlowSpec extends TestBase with OptionValues {

  trait StubRepo {
    import sys.process._
    val repo               = Files.createTempDirectory("restui-git-test")
    private val hideStdOut = ProcessLogger(_ => ())

    Process(Seq("git", "init"), repo.toFile()).!(hideStdOut) shouldBe 0
    commit("init", "init")

    def commit(path: String, text: String): Unit = {
      val file = Files.write(repo.resolve(path), text.getBytes())
      Process(Seq("git", "add", file.toAbsolutePath.toString), repo.toFile()).!(hideStdOut)
      Process(Seq("git", "commit", "-m", "new file"), repo.toFile()).!(hideStdOut)
    }

    def sha1(branch: String): String =
      Process(Seq("git", "rev-parse", "--verify", branch), repo.toFile()).!!(hideStdOut).trim

  }

  "Executing a git flow" when {
    "there is a failure with a git command" in {
      val fixture = new StubRepo {}
      val tempDir = Files.createTempDirectory("restui-git-test-clone").toFile
      val repo    = Repo(fixture.repo.toAbsolutePath.toString, "i-do-not-exists", tempDir, List("test"))

      Source.single(repo).via(GitFlow.flow).runWith(Sink.seq).map { result =>
        result shouldBe empty
      }
    }

    "the repo has been initialised for the time" should {

      "not find files" when {

        "there is no matching files" in {
          val fixture = new StubRepo {}
          val tempDir = Files.createTempDirectory("restui-git-test-clone").toFile
          val repo    = Repo(fixture.repo.toAbsolutePath.toString, "master", tempDir, List("test"))

          Source.single(repo).via(GitFlow.flow).runWith(Sink.seq).map { result =>
            result should have length 1
            val (repo, files) = result(0)
            repo.cachedRef should be('defined)
            files shouldBe empty
          }
        }
      }

      "find files" in {
        val fixture = new StubRepo {}
        fixture.commit("test", "test")
        val tempDir = Files.createTempDirectory("restui-git-test-clone").toFile
        val repo    = Repo(fixture.repo.toAbsolutePath.toString, "master", tempDir, List("test"))

        Source.single(repo).via(GitFlow.flow).runWith(Sink.seq).map { result =>
          result should have length 1
          val (repo, files) = result(0)
          repo.cachedRef should be('defined)
          files shouldBe List(OpenApiFile(ContentTypes.Plain, "test"))
        }
      }
    }

    "the repo has already been initialised" should {

      "not find files" in {
        val fixture = new StubRepo {}
        val tempDir = Files.createTempDirectory("restui-git-test-clone").toFile
        fixture.commit("test", "test")
        val ref  = fixture.sha1("master")
        val repo = Repo(fixture.repo.toAbsolutePath.toString, "master", tempDir, List("test"), Some(ref))
        fixture.commit("test2", "test")
        Source.single(repo).via(GitFlow.flow).runWith(Sink.seq).map { result =>
          result should have length 1
          val (repo, files) = result(0)
          repo.cachedRef should be('defined)
          files shouldBe empty
        }
      }
    }
  }

}
