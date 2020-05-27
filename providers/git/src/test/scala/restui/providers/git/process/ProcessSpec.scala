package restui.providers.git.process

import java.nio.file.Files

import akka.stream.scaladsl.{Sink, Source}
import base.TestBase
import org.scalatest.EitherValues
import restui.providers.git.process.Process

class ProcessSpec extends TestBase with EitherValues {
  "Executing an existing command" when {

    "not providing a current directory" in {
      val args = ProcessArgs("echo" :: """1
        |2""".stripMargin :: Nil)
      Source.single(args).via(Process.execute).runWith(Sink.seq).map { result =>
        result should have length 1
        result.head.right.value shouldBe List("1", "2")
      }
    }

    "providing a current directory" in {
      val tempDir = Files.createTempDirectory("process-execute-test").toFile
      val args    = ProcessArgs("pwd" :: Nil, Some(tempDir))
      Source.single(args).via(Process.execute).runWith(Sink.seq).map { result =>
        result should have length 1
        val path = tempDir.getCanonicalPath()
        tempDir.delete()
        result.head.right.value shouldBe List(path)
      }
    }
  }
  "Executing a non existing command" in {
    val args = ProcessArgs("i_do_not_exists" :: Nil)
    Source.single(args).via(Process.execute).runWith(Sink.seq).map { result =>
      result should have length 1
      result.head.left.value shouldBe ""
    }
  }
}
