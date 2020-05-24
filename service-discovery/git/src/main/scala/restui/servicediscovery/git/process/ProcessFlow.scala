package restui.servicediscovery.git.process

import scala.sys.process._
import scala.util.Try

import akka.stream.scaladsl.{Flow => AkkaFlow}
import restui.servicediscovery.git.Flow

object ProcessFlow {

  val flow: Flow[ProcessArgs, Either[String, List[String]]] =
    AkkaFlow[ProcessArgs].map { processArgs =>
      val buffer = new StringBuffer()
      val logger = ProcessLogger(buffer append _)
      Try {
        processArgs.workingDirectory
          .fold(processArgs.args.lazyLines(logger)) { workingDirectory =>
            Process(processArgs.args, workingDirectory).lazyLines(logger)
          }
          .toList
      }.toEither.left.map(_ => buffer.toString)
    }
}
