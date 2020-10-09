package restui.models

import io.circe.syntax._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.prop.TableDrivenPropertyChecks
import restui.models.Event._

class EventSpec
    extends AnyFlatSpec
    with Matchers
    with TableDrivenPropertyChecks {
  it should "serialise the event as a valid json string" in {
    val properties = Table(
      ("event", "json"),
      (Event.ServiceUp(
         Event.Service.OpenApi("id", "test", false, Map("key" -> "value"))),
       """{"event":"serviceUp","id":"id","name":"test","metadata":{"key":"value"},"useProxy":false,"type":"openapi"}"""),
      (Event.ServiceUp(Event.Service.Grpc("id", "test", Map("key" -> "value"))),
       """{"event":"serviceUp","id":"id","name":"test","metadata":{"key":"value"},"type":"grpc"}"""),
      (Event.ServiceDown("id"), """{"event":"serviceDown","id":"id"}""")
    )

    forAll(properties) { (event: Event, json) =>
      event.asJson.noSpaces shouldBe json
    }
  }
  it should "serialise a list of events as a valid json string" in {
    val properties = Table(
      ("event", "json"),
      (List(
         Event.ServiceUp(
           Event.Service.OpenApi("id", "test", false, Map("key" -> "value")))),
       """[{"event":"serviceUp","id":"id","name":"test","metadata":{"key":"value"},"useProxy":false,"type":"openapi"}]""")
    )

    forAll(properties) { (events, json) =>
      events.asJson.noSpaces shouldBe json
    }
  }

}
