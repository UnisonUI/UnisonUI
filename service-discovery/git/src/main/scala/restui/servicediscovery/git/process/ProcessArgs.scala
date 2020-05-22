package restui.servicediscovery.git.process

import java.io.File

final case class ProcessArgs(args: Seq[String], workingDirectory: Option[File] = None)
