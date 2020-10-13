package restui.server.http.directives

import akka.http.scaladsl.model.headers.{
  `Accept-Encoding`,
  `Content-Encoding`,
  HttpEncodingRange,
  HttpEncodings
}
import akka.http.scaladsl.server.Directives
import akka.http.scaladsl.testkit.ScalatestRouteTest
import org.scalatest.OptionValues
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import restui.server.http.directives.Encodings

class StaticsDirectivesSpec
    extends AnyWordSpec
    with Matchers
    with ScalatestRouteTest
    with Directives
    with StaticsDirectives
    with OptionValues {
  private val route = get(
    staticsFromResourceDirectory("statics", Encodings.Brotli, Encodings.Gzip))
  private val fileContent = "Test\n"

  "Get a non compressed file" when {
    "there is none" in {
      Get("/test2.txt") ~> addHeader(
        `Accept-Encoding`(HttpEncodingRange.*)) ~> route ~> check {
        header[`Content-Encoding`] should not be Symbol("defined")
        responseAs[String] shouldBe fileContent
      }
    }

    "there is one but not the corresponding header" in {
      Get("/test.txt") ~> addHeader(
        `Accept-Encoding`(
          HttpEncodingRange(HttpEncodings.deflate))) ~> route ~> check {
        header[`Content-Encoding`] should not be Symbol("defined")
        responseAs[String] shouldBe fileContent
      }
    }
  }

  "Get a compressed file" when {
    "it's the first matching" in {
      Get("/test.txt") ~> addHeader(
        `Accept-Encoding`(HttpEncodingRange.*)) ~> route ~> check {
        header[`Content-Encoding`].value.encodings shouldBe Seq(
          Encodings.Brotli.encoding)
      }
    }
    "it's the secongs matching" in {
      Get("/test3.txt") ~> addHeader(
        `Accept-Encoding`(HttpEncodingRange.*)) ~> route ~> check {
        header[`Content-Encoding`].value.encodings shouldBe Seq(
          Encodings.Gzip.encoding)
      }
    }
  }
}
