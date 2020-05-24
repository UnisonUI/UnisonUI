package restui.servicediscovery.git.git

import java.io.File

package object data {
  final case class Repository(uri: String,
                              branch: String,
                              swaggerPaths: List[String],
                              directory: Option[File] = None,
                              cachedRef: Option[String] = None)
  final case class RestUI(swaggers: List[String])
}
