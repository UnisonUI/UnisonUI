package base

import scala.concurrent.ExecutionContext

import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.{AnyWordSpecLike, AsyncWordSpecLike}

abstract class AsyncTestBase extends ScalaTestWithActorTestKit with AsyncWordSpecLike with Matchers
abstract class TestBase extends ScalaTestWithActorTestKit with AnyWordSpecLike with Matchers {
  implicit val executionContext: ExecutionContext = testKit.system.executionContext
}
