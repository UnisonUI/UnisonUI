package restui.server.http

import java.nio.charset.StandardCharsets
import java.{util => ju}

import scala.util.chaining._

object Base64 {
  def encode(input: String): String =
    input
      .getBytes(StandardCharsets.UTF_8)
      .pipe(ju.Base64.getEncoder.encodeToString)

  def decode(input: String): String =
    new String(
      input
        .replaceAll("_", "/")
        .getBytes(StandardCharsets.UTF_8)
        .pipe(ju.Base64.getDecoder.decode))

}
