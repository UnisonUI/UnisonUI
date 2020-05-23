package restui.servicediscovery.git.git

import java.io.File

final case class Repo(uri: String, branch: String, directory: File, swaggerPaths: List[String], cachedRef: Option[String] = None)
