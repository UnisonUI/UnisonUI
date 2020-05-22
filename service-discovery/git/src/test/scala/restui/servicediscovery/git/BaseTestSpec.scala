package restui.servicediscovery.git

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestKit}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AsyncWordSpecLike

abstract class BaseTestSpec
    extends TestKit(ActorSystem("test"))
    with ImplicitSender
    with AsyncWordSpecLike
    with Matchers
    with BeforeAndAfterAll {
  override def afterAll: Unit =
    TestKit.shutdownActorSystem(system)
}
