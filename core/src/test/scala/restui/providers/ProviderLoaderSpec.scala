package restui.providers

import akka.NotUsed
import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import akka.actor.typed.ActorSystem
import akka.stream.scaladsl.{Sink, Source}
import com.typesafe.config.{Config, ConfigFactory}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import restui.models.ServiceEvent

import scala.reflect.ClassTag
class ProviderLoaderSpec
    extends ScalaTestWithActorTestKit
    with AnyWordSpecLike
    with Matchers {
  private def setupProvider[T](implicit t: ClassTag[T]): (String, Config) = {
    val className = t.runtimeClass.getCanonicalName
    System.setProperty("restui.providers.0", className)
    ConfigFactory.invalidateCaches()
    className -> ConfigFactory.load()
  }

  "Load a provider" when {
    "the provider start successfully" in {
      val (className, config) = setupProvider[StubProvider]
      val probe               = testKit.createTestProbe[(String, ServiceEvent)]()
      ProvidersLoader.load(config).to(Sink.foreach(e => probe.ref ! e)).run()
      probe.expectMessage(className -> ServiceEvent.ServiceDown("test"))
    }

    "the provider don't start successfully" in {
      val (_, config) = setupProvider[FailedStubProvider]
      val probe       = testKit.createTestProbe[(String, ServiceEvent)]()
      ProvidersLoader.load(config).to(Sink.foreach(e => probe.ref ! e)).run()
      probe.expectNoMessage()
    }

  }
}
class StubProvider extends Provider {
  override def start(actorSystem: ActorSystem[_],
                     config: Config): Source[(String, ServiceEvent), NotUsed] =
    Source.single(
      classOf[StubProvider].getCanonicalName -> ServiceEvent.ServiceDown(
        "test"))
}

class FailedStubProvider extends Provider {
  override def start(actorSystem: ActorSystem[_],
                     config: Config): Source[(String, ServiceEvent), NotUsed] =
    throw new Exception("test")
}
