package restui.servicediscovery.git.process

import scala.sys.process.{Process => ScalaProcess, _}
import scala.util.Try

import akka.stream.scaladsl.{Flow => AkkaFlow}
import restui.servicediscovery.git.Flow

object Process {

  val execute: Flow[ProcessArgs, Either[String, List[String]]] =
    AkkaFlow[ProcessArgs].map { processArgs =>
      val buffer = new StringBuffer()
      val logger = ProcessLogger(buffer append _)
      Try {
        processArgs.workingDirectory
          .fold(processArgs.args.lazyLines(logger)) { workingDirectory =>
            ScalaProcess(processArgs.args, workingDirectory).lazyLines(logger)
          }
          .toList
      }.toEither.left.map(_ => buffer.toString)
    }
}
