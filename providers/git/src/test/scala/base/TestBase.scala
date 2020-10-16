package base

import java.io.File
import java.nio.file.Path

import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import cats.syntax.either._
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.{AnyWordSpecLike, AsyncWordSpecLike}
import restui.protobuf.ProtobufCompiler

import scala.concurrent.ExecutionContext
abstract class AsyncTestBase
    extends ScalaTestWithActorTestKit
    with AsyncWordSpecLike
    with Matchers
abstract class TestBase
    extends ScalaTestWithActorTestKit
    with AnyWordSpecLike
    with Matchers {
  implicit val compiler: ProtobufCompiler = new ProtobufCompiler {
    override def compile(path: Path): Either[Throwable, File] =
      new File(s"${path.toAbsolutePath.toString}set").asRight[Throwable]
    override def clean(file: File): Either[Throwable, Unit] = ().asRight
  }
  implicit val executionContext: ExecutionContext =
    testKit.system.executionContext
}
