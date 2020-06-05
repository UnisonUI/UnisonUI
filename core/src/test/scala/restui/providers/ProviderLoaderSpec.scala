package restui.providers

import scala.reflect.ClassTag
import scala.util.Try

import akka.actor.ActorSystem
import akka.testkit.{TestKit, TestProbe}
import com.typesafe.config.{Config, ConfigFactory}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import restui.models.ServiceEvent

class ProviderLoaderSpec extends TestKit(ActorSystem("test")) with AnyWordSpecLike with Matchers {
  private def setupProvider[T](implicit t: ClassTag[T]): (String, Config) = {
    val className = t.runtimeClass.getCanonicalName
    System.setProperty("restui.providers.0", className)
    ConfigFactory.invalidateCaches()
    className -> ConfigFactory.load()
  }

  "Load a provider" when {
    "the provider start successfully" in {
      val (className, config) = setupProvider[StubProvider]
      val probe               = TestProbe()
      ProvidersLoader.load(config, provider => event => probe.ref ! (provider, event))
      probe.expectMsg(className -> ServiceEvent.ServiceDown("test"))
    }

    "the provider don't start successfully" in {
      val (_, config) = setupProvider[FailedStubProvider]
      val probe       = TestProbe()
      ProvidersLoader.load(config, provider => event => probe.ref ! (provider, event))
      probe.expectNoMessage
    }

  }
}
class StubProvider extends Provider {
  override def start(actorSystem: ActorSystem, config: Config, callback: Provider.Callback): Try[Unit] =
    Try {
      callback(ServiceEvent.ServiceDown("test"))
    }
}

class FailedStubProvider extends Provider {
  override def start(actorSystem: ActorSystem, config: Config, callback: Provider.Callback): Try[Unit] = Try(throw new Exception("test"))
}
