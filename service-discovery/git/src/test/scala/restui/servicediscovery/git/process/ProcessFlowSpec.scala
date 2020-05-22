package restui.servicediscovery.git.process

import java.nio.file.Files

import akka.stream.scaladsl.{Sink, Source}
import org.scalatest.TryValues
import restui.servicediscovery.git.BaseTestSpec
import restui.servicediscovery.git.process.ProcessFlow.flow

class ProcessFlowSpec extends BaseTestSpec with TryValues {
  "Executing an existing command" when {
    "not providing a current directory" in {
      val args = ProcessArgs("echo" :: """1
        |2""".stripMargin :: Nil)
      Source.single(args).via(flow).runWith(Sink.seq).map { result =>
        result should have length 1
        result(0).success.value shouldBe List("1", "2")
      }
    }
    "providing a current directory" in {
      val tempDir = Files.createTempDirectory("process-flow-test").toFile
      val args    = ProcessArgs("pwd" :: Nil, Some(tempDir))
      Source.single(args).via(flow).runWith(Sink.seq).map { result =>
        result should have length 1
        val path = tempDir.getCanonicalPath()
        tempDir.delete()
        result(0).success.value shouldBe List(path)
      }
    }
  }
}
