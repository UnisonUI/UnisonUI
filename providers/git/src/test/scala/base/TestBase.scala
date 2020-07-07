package base

import scala.concurrent.ExecutionContext

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestKit}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.{AnyWordSpecLike, AsyncWordSpecLike}

abstract class AsyncTestBase
    extends TestKit(ActorSystem("test"))
    with ImplicitSender
    with AsyncWordSpecLike
    with Matchers
    with BeforeAndAfterAll {
  implicit val ec: ExecutionContext = system.dispatcher

  override def afterAll(): Unit =
    TestKit.shutdownActorSystem(system)
}

abstract class TestBase extends TestKit(ActorSystem("test")) with ImplicitSender with AnyWordSpecLike with Matchers with BeforeAndAfterAll {
  implicit val ec: ExecutionContext = system.dispatcher

  override def afterAll(): Unit =
    TestKit.shutdownActorSystem(system)
}
