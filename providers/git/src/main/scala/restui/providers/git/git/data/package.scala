package restui.providers.git.git

import java.io.File

package object data {
  final case class Repository(uri: String,
                              branch: String,
                              specificationPaths: List[String],
                              directory: Option[File] = None,
                              serviceName: Option[String] = None)
  final case class RestUI(name: Option[String], specifications: List[String])
}
