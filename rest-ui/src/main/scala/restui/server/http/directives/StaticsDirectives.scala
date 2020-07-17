package restui.server.http.directives
import scala.annotation.tailrec

import akka.actor.ActorSystem
import akka.event.LoggingAdapter
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.{`Accept-Encoding`, `Content-Encoding`, Range}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route

trait StaticsDirectives {
  import akka.http.scaladsl.server.directives.BasicDirectives._
  import akka.http.scaladsl.server.directives.FileAndResourceDirectives.getFromResource

  private val classLoader = classOf[ActorSystem].getClassLoader

  def staticsFromResourceDirectory(directory: String, first: Encoding, other: Encoding*): Route =
    staticsFromResourceDirectory(directory, Seq(first +: other: _*))

  def staticsFromResourceDirectory(directory: String, encodings: Seq[Encoding]): Route = {
    val base = withTrailingSlash(directory)
    extractRequest { request =>
      extractUnmatchedPath { path =>
        extractLog { log =>
          val hasRangeHeader     = request.header[Range].isDefined
          val availableEncodings = getAvailableEncodings(request, encodings)
          safeJoinPaths(base, path, log, separator = '/') match {
            case "" => reject
            case resourceName =>
              serveFile(resourceName, hasRangeHeader, availableEncodings)
          }
        }
      }
    }
  }

  private def getAvailableEncodings(request: HttpRequest, encodings: Seq[Encoding]): Seq[Encoding] =
    (for {
      acceptEncoding <- request.header[`Accept-Encoding`]
      acceptEncodings    = acceptEncoding.encodings
      availableEncodings = encodings.filter(encoding => acceptEncodings.exists(_.matches(encoding.encoding)))
    } yield availableEncodings).getOrElse(Seq.empty[Encoding])

  private def withTrailingSlash(path: String): String = if (path endsWith "/") path else path + '/'

  private def serveFile(resourceName: String, isRange: Boolean, availableEncodings: Seq[Encoding]): Route = {
    val contentType = ContentType(MediaTypes.forExtension(resourceName.split('.').last), () => HttpCharsets.`UTF-8`)
    if (isRange) getFromResource(resourceName)
    else
      availableEncodings.collectFirst {
        case encoding if Option(classLoader.getResource(s"$resourceName.${encoding.extension}")).isDefined =>
          respondWithHeader(`Content-Encoding`(encoding.encoding)) {
            getFromResource(s"$resourceName.${encoding.extension}", contentType, classLoader)
          }
      }.getOrElse(getFromResource(resourceName))
  }

  private def safeJoinPaths(base: String, path: Uri.Path, log: LoggingAdapter, separator: Char): String = {
    import java.lang.StringBuilder
    @tailrec def rec(p: Uri.Path, result: StringBuilder = new StringBuilder(base)): String =
      p match {
        case Uri.Path.Empty       => result.toString
        case Uri.Path.Slash(tail) => rec(tail, result.append(separator))
        case Uri.Path.Segment(head, tail) =>
          if (head.indexOf('/') >= 0 || head.indexOf('\\') >= 0 || head == "..") {
            log.warning("File-system path for base [{}] and Uri.Path [{}] contains suspicious path segment [{}], " +
                          "GET access was disallowed",
                        base,
                        path,
                        head)
            ""
          } else rec(tail, result.append(head))
      }
    rec(if (path.startsWithSlash) path.tail else path)
  }
}

object StaticsDirectives extends StaticsDirectives
