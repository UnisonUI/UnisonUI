package restui.server

import java.nio.charset.StandardCharsets
import java.{util => ju}

import scala.util.chaining._

object Base64 {
  def encode(input: String): String =
    input
      .getBytes(StandardCharsets.UTF_8)
      .pipe(ju.Base64.getEncoder.encodeToString)
      .replace('/', '_')

  def decode(input: String): String = new String(input.replace('_', '/').getBytes(StandardCharsets.UTF_8).pipe(ju.Base64.getDecoder.decode))

}
