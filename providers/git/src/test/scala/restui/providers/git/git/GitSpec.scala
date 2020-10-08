package restui.providers.git.git

import java.nio.file.{Files, Path, Paths}

import scala.concurrent.duration._

import akka.stream.scaladsl.{Sink, Source}
import base.TestBase
import org.scalatest.Inside
import restui.models.{Metadata, Service, ServiceEvent}
import restui.protobuf.data.Schema
import restui.providers.git.git.data._

class GitSpec extends TestBase with Inside {
  private val duration  = 50.millis
  private val specs     = """specifications:
           |  - test2
           |""".stripMargin
  private val grpcSpecs = """specifications:
           |  - test
           |grpc:
           | "helloworld.proto":
           |    servers:
           |      - address: 127.0.0.1
           |        port: 8080
           |""".stripMargin
  trait StubRepository {
    import sys.process._
    val repo       = Files.createTempDirectory("restui-git-test")
    val hideStdErr = ProcessLogger(_ => ())

    Process(Seq("git", "init"), repo.toFile()).!(hideStdErr) shouldBe 0
    Process(Seq("git", "config", "user.email", "test@test.org"), repo.toFile()).!(hideStdErr)
    Process(Seq("git", "config", "user.name", "test"), repo.toFile()).!(hideStdErr)

    commit("init", "init")

    def commit(file: Path): Unit = {
      Process(Seq("git", "add", file.toAbsolutePath.toString), repo.toFile()).!(hideStdErr)
      Process(Seq("git", "commit", "-m", "new file"), repo.toFile()).!(hideStdErr)
    }

    def commit(path: String, text: String): Unit = {
      val file = Files.write(repo.resolve(path), text.getBytes())
      commit(file)
    }

    def rm(path: String): Unit = {
      Process(Seq("git", "rm", repo.resolve(path).toAbsolutePath.toString), repo.toFile()).!(hideStdErr)
      Process(Seq("git", "commit", "-m", "rm file"), repo.toFile()).!(hideStdErr)
    }

    def mv(path: String, newPath: String): Unit = {
      Process(Seq("git", "mv", repo.resolve(path).toAbsolutePath.toString, repo.resolve(newPath).toAbsolutePath.toString), repo.toFile())
        .!(hideStdErr)
      Process(Seq("git", "commit", "-m", "mv file"), repo.toFile()).!(hideStdErr)
    }

    def sha1(branch: String): String =
      Process(Seq("git", "rev-parse", "--verify", branch), repo.toFile()).!!(hideStdErr).trim

  }

  "Executing a git flow" when {
    "there is a failure with a git command" in {
      val fixture = new StubRepository {}
      val tempDir = Files.createTempDirectory("restui-git-test-clone").toFile
      val repo    = Repository(s"file://${fixture.repo.toAbsolutePath}", "i-do-not-exists", List(UnnamedSpecification("test")), Some(tempDir))

      val probe = testKit.createTestProbe[ServiceEvent]()
      Git.fromSource(duration, Source.single(repo)).to(Sink.foreach(e => probe.ref ! e)).run()
      probe.expectNoMessage()
    }

    "retrieving files from git" should {

      "not find files" when {

        "there is no matching files" in {
          val fixture = new StubRepository {}
          val tempDir = Files.createTempDirectory("restui-git-test-clone").toFile
          val repo    = Repository(s"file://${fixture.repo.toAbsolutePath}", "master", List(UnnamedSpecification("test")), Some(tempDir))

          val probe = testKit.createTestProbe[ServiceEvent]()
          Git.fromSource(duration, Source.single(repo)).to(Sink.foreach(e => probe.ref ! e)).run()
          probe.expectNoMessage()
        }
      }

      "find files" when {
        "no new file is present" in {
          val fixture = new StubRepository {}
          fixture.commit("test", "test")
          val tempDir = Files.createTempDirectory("restui-git-test-clone").toFile
          val repo    = Repository(s"file://${fixture.repo.toAbsolutePath}", "master", List(UnnamedSpecification("test")), Some(tempDir))

          val probe = testKit.createTestProbe[ServiceEvent]()
          Git.fromSource(duration, Source.single(repo)).to(Sink.foreach(e => probe.ref ! e)).run()
          val result = probe.expectMessageType[ServiceEvent]
          inside(result) {
            case ServiceEvent.ServiceUp(Service.OpenApi(_, _, file, _, _, _)) =>
              file shouldBe "test"
          }
        }

        "a new file is present" in {
          val fixture = new StubRepository {}
          fixture.commit("test", "test")
          val tempDir = Files.createTempDirectory("restui-git-test-clone").toFile
          val repo    = Repository(s"file://${fixture.repo.toAbsolutePath}", "master", List(UnnamedSpecification("test")), Some(tempDir))

          val probe = testKit.createTestProbe[ServiceEvent]()
          Git.fromSource(duration, Source.single(repo)).to(Sink.foreach(e => probe.ref ! e)).run()

          inside(probe.expectMessageType[ServiceEvent]) {
            case ServiceEvent.ServiceUp(Service.OpenApi(_, _, file, _, _, _)) =>
              file shouldBe "test"
          }

          fixture.commit("test", "test2")

          inside(probe.expectMessageType[ServiceEvent]) {
            case ServiceEvent.ServiceUp(Service.OpenApi(_, _, file, _, _, _)) =>
              file shouldBe "test2"
          }

        }

        "a file has been deleted" in {
          val fixture = new StubRepository {}
          fixture.commit("test", "test")
          val tempDir = Files.createTempDirectory("restui-git-test-clone").toFile
          val repo    = Repository(s"file://${fixture.repo.toAbsolutePath}", "master", List(UnnamedSpecification("test")), Some(tempDir))

          val probe = testKit.createTestProbe[ServiceEvent]()
          Git.fromSource(duration, Source.single(repo)).to(Sink.foreach(e => probe.ref ! e)).run()

          inside(probe.expectMessageType[ServiceEvent]) {
            case ServiceEvent.ServiceUp(Service.OpenApi(_, _, file, _, _, _)) =>
              file shouldBe "test"
          }

          fixture.rm("test")

          probe.expectMessageType[ServiceEvent.ServiceDown]
        }

        "the specifications changed" in {
          val fixture = new StubRepository {}
          fixture.commit("test", "test")
          val tempDir = Files.createTempDirectory("restui-git-test-clone").toFile
          val repo    = Repository(s"file://${fixture.repo.toAbsolutePath}", "master", List(UnnamedSpecification("test")), Some(tempDir))

          val probe = testKit.createTestProbe[ServiceEvent]()
          Git.fromSource(duration, Source.single(repo)).to(Sink.foreach(e => probe.ref ! e)).run()
          inside(probe.expectMessageType[ServiceEvent]) {
            case ServiceEvent.ServiceUp(Service.OpenApi(_, _, file, _, _, _)) =>
              file shouldBe "test"
          }

          fixture.commit(".restui.yaml", specs)

          probe.expectMessageType[ServiceEvent.ServiceDown]
        }

        "the file has been renamed" in {
          val fixture = new StubRepository {}
          fixture.commit("test", "test")
          val tempDir = Files.createTempDirectory("restui-git-test-clone").toFile
          val repo    = Repository(s"file://${fixture.repo.toAbsolutePath}", "master", List(UnnamedSpecification("test")), Some(tempDir))

          val probe = testKit.createTestProbe[ServiceEvent]()
          Git.fromSource(duration, Source.single(repo)).to(Sink.foreach(e => probe.ref ! e)).run()

          inside(probe.expectMessageType[ServiceEvent]) {
            case ServiceEvent.ServiceUp(Service.OpenApi(_, _, _, metadata, _, _)) =>
              metadata should contain(Metadata.File -> "test")
          }

          fixture.commit(".restui.yaml", specs)
          fixture.mv("test", "test2")

          probe.expectMessageType[ServiceEvent.ServiceDown]

          inside(probe.expectMessageType[ServiceEvent]) {
            case ServiceEvent.ServiceUp(Service.OpenApi(_, _, _, metadata, _, _)) =>
              metadata should contain(Metadata.File -> "test2")
          }

        }
        "the files is a protobuf" when {
          "it is mixed with OpenApi spec" in {
            val fixture = new StubRepository {}
            fixture.commit("test", "test")
            val tempDir = Files.createTempDirectory("restui-git-test-clone").toFile
            val repo    = Repository(s"file://${fixture.repo.toAbsolutePath}", "master", List(UnnamedSpecification("test")), Some(tempDir))

            val probe = testKit.createTestProbe[ServiceEvent]()
            Git.fromSource(duration, Source.single(repo)).to(Sink.foreach(e => probe.ref ! e)).run()
            inside(probe.expectMessageType[ServiceEvent]) {
              case ServiceEvent.ServiceUp(Service.OpenApi(_, _, file, _, _, _)) =>
                file shouldBe "test"
            }

            fixture.commit(".restui.yaml", grpcSpecs)
            val path    = Paths.get("src/test/resources/helloworld.proto")
            val newPath = fixture.repo.resolve("helloworld.proto")
            Files.copy(path, newPath)
            fixture.commit(newPath)

            inside(probe.expectMessageType[ServiceEvent]) {
              case ServiceEvent.ServiceUp(Service.Grpc(_, _, schema, servers, _)) =>
                servers shouldBe Map("127.0.0.1:8080" -> Service.Grpc.Server("127.0.0.1", 8080, false))
                inside(schema) {
                  case Schema(messages, enums, services, None) =>
                    messages should have size 2
                    enums shouldBe Symbol("empty")
                    services should have size 1
                }
            }
          }
        }
      }
    }
  }

}
