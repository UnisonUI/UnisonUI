package restui.models

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.prop.TableDrivenPropertyChecks

class ContentTypeSpec extends AnyFlatSpec with Matchers with TableDrivenPropertyChecks {
  private val properties = Table(
    ("file", "type"),
    ("test.yaml", ContentType.Yaml),
    ("test.yml", ContentType.Yaml),
    ("test.json", ContentType.Json),
    ("test.txt", ContentType.Plain)
  )
  it should "get the correct content type" in {
    forAll(properties) { (filename, fileType) =>
      ContentType.fromString(filename) shouldBe fileType
    }
  }
}
