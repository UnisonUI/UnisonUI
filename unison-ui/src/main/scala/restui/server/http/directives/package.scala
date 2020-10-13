package restui.server.http

import akka.http.scaladsl.model.headers.{HttpEncoding, HttpEncodings}

package object directives {
  trait Encoding {
    val encoding: HttpEncoding
    val extension: String
  }
  object Encodings {
    object Brotli extends Encoding {
      override val encoding: HttpEncoding = HttpEncoding.custom("br")
      override val extension: String      = "br"
    }

    object Gzip extends Encoding {
      override val encoding: HttpEncoding = HttpEncodings.gzip
      override val extension: String      = "gz"
    }
  }
}
