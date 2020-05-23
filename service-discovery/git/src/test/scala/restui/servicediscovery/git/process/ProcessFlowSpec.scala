package restui.servicediscovery.git.process

import java.nio.file.Files

import akka.stream.scaladsl.{Sink, Source}
import base.TestBase
import org.scalatest.EitherValues
import restui.servicediscovery.git.process.ProcessFlow.flow

class ProcessFlowSpec extends TestBase with EitherValues {
  "Executing an existing command" when {

    "not providing a current directory" in {
      val args = ProcessArgs("echo" :: """1
        |2""".stripMargin :: Nil)
      Source.single(args).via(flow).runWith(Sink.seq).map { result =>
        result should have length 1
        result(0).right.value shouldBe List("1", "2")
      }
    }

    "providing a current directory" in {
      val tempDir = Files.createTempDirectory("process-flow-test").toFile
      val args    = ProcessArgs("pwd" :: Nil, Some(tempDir))
      Source.single(args).via(flow).runWith(Sink.seq).map { result =>
        result should have length 1
        val path = tempDir.getCanonicalPath()
        tempDir.delete()
        result(0).right.value shouldBe List(path)
      }
    }
  }
  "Executing a non existing command" in {
    val args = ProcessArgs("i_do_not_exists" :: Nil)
    Source.single(args).via(flow).runWith(Sink.seq).map { result =>
      result should have length 1
      result(0).left.value shouldBe ""
    }
  }
}
