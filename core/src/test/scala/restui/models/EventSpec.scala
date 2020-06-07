package restui.models

import io.circe.syntax._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.prop.TableDrivenPropertyChecks
import restui.models.Event._

class EventSpec extends AnyFlatSpec with Matchers with TableDrivenPropertyChecks {
  it should "serialise the event as a valid json string" in {
    val properties = Table(
      ("event", "json"),
      (Event.ServiceUp("id", "test", Map("key" -> "value")),
       """{"event":"serviceUp","id":"id","name":"test","metadata":{"key":"value"}}"""),
      (Event.ServiceDown("id"), """{"event":"serviceDown","id":"id"}""")
    )

    forAll(properties) { (event: Event, json) =>
      event.asJson.noSpaces shouldBe json
    }
  }
  it should "serialise a list of events as a valid json string" in {
    val properties = Table(
      ("event", "json"),
      (List(Event.ServiceUp("id", "test", Map("key" -> "value"))),
       """[{"event":"serviceUp","id":"id","name":"test","metadata":{"key":"value"}}]""")
    )

    forAll(properties) { (events, json) =>
      events.asJson.noSpaces shouldBe json
    }
  }

}
