package restui.models

import io.circe.syntax._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.prop.TableDrivenPropertyChecks
import restui.models.Event._

class EventSpec extends AnyFlatSpec with Matchers with TableDrivenPropertyChecks {
  private val properties = Table(
    ("event", "json"),
    (Event.ServiceUp("id", "test", Map("key" -> "value")), """{"event":"serviceUp","id":"id","name":"test","metadata":{"key":"value"}}"""),
    (Event.ServiceDown("id"), """{"event":"serviceDown","id":"id"}"""),
    (List(Event.ServiceUp("id", "test", Map("key" -> "value"))),
     """[{"event":"serviceUp","id":"id","name":"test","metadata":{"key":"value"}}]""")
  )
  it should "serialise the event as a valid json string" in {
    forAll(properties) { (input: Any, json) =>
      val computedJson = input match {
        case event: Event                  => event.asJson
        case events: List[Event.ServiceUp] => events.asJson
      }
      computedJson.noSpaces shouldBe json
    }
  }
}
