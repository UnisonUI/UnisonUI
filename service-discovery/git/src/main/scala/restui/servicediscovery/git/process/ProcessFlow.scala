package restui.servicediscovery.git.process

import scala.sys.process._
import scala.util.Try

import akka.NotUsed
import akka.stream.scaladsl.Flow

object ProcessFlow {

  val flow: Flow[ProcessArgs, Try[List[String]], NotUsed] =
    Flow[ProcessArgs].map { processArgs =>
      Try {
        processArgs.workingDirectory
          .fold(processArgs.args.lazyLines) { workingDirectory =>
            Process(processArgs.args, workingDirectory).lazyLines
          }
          .toList
      }
    }
}
