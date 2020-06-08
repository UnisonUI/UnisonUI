package restui.providers

import scala.reflect.ClassTag

import akka.actor.ActorSystem
import akka.stream.scaladsl.{Sink, Source}
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
      ProvidersLoader.load(config).to(Sink.actorRef(probe.ref, "completed", _ => ())).run()
      probe.expectMsg(className -> ServiceEvent.ServiceDown("test"))
    }

    "the provider don't start successfully" in {
      val (_, config) = setupProvider[FailedStubProvider]
      val probe       = TestProbe()
      ProvidersLoader.load(config).to(Sink.actorRef(probe.ref, "completed", _ => ())).run()
      probe.expectMsg("completed")
    }

  }
}
class StubProvider extends Provider {
  override def start(actorSystem: ActorSystem, config: Config): Provider.StreamingSource =
    Source.single(classOf[StubProvider].getCanonicalName -> ServiceEvent.ServiceDown("test"))
}

class FailedStubProvider extends Provider {
  override def start(actorSystem: ActorSystem, config: Config): Provider.StreamingSource = throw new Exception("test")
}
