package restui.servicediscovery.docker

import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.should.Matchers

class DockerClientSpec extends AnyWordSpec with Matchers {
  "A list" should {
    "not be empty" in {
      List(1) should not be empty
    }
  }
}

