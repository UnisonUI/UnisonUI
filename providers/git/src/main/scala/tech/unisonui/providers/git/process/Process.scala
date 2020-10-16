package tech.unisonui.providers.git.process

import akka.stream.scaladsl.{Flow => AkkaFlow}
import tech.unisonui.providers.git.Flow

import scala.sys.process.{Process => ScalaProcess, _}
import scala.util.Try

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
