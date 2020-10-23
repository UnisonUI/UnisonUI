package tech.unisonui.server.http.directives

import java.io.File

import akka.event.LoggingAdapter
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.{
  `Accept-Encoding`,
  `Content-Encoding`,
  Range
}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import com.typesafe.scalalogging.LazyLogging

import scala.annotation.tailrec

trait StaticsDirectives extends LazyLogging {
  import akka.http.scaladsl.server.directives.BasicDirectives._

  def staticsFromDirectory(directory: String,
                           first: Encoding,
                           other: Encoding*): Route =
    staticsFromDirectory(directory, Seq(first +: other: _*))

  def staticsFromDirectory(directory: String,
                           encodings: Seq[Encoding]): Route = {
    val base = withTrailingSlash(directory)
    extractRequest { request =>
      extractUnmatchedPath { path =>
        extractLog { log =>
          val hasRangeHeader     = request.header[Range].isDefined
          val availableEncodings = getAvailableEncodings(request, encodings)
          safeJoinPaths(base, path, log) match {
            case "" => reject
            case resourceName =>
              serveFile(resourceName, hasRangeHeader, availableEncodings)
          }
        }
      }
    }
  }

  private def getAvailableEncodings(request: HttpRequest,
                                    encodings: Seq[Encoding]): Seq[Encoding] =
    (for {
      acceptEncoding <- request.header[`Accept-Encoding`]
      acceptEncodings = acceptEncoding.encodings
      availableEncodings = encodings.filter(encoding =>
        acceptEncodings.exists(_.matches(encoding.encoding)))
    } yield availableEncodings).getOrElse(Seq.empty[Encoding])

  private def withTrailingSlash(path: String): String =
    if (path endsWith "/") path else path + '/'

  private def serveFile(path: String,
                        isRange: Boolean,
                        availableEncodings: Seq[Encoding]): Route = {
    val contentType = ContentType(MediaTypes.forExtension(path.split('.').last),
                                  () => HttpCharsets.`UTF-8`)
    if (isRange) getFromFile(path)
    else
      availableEncodings
        .map(encoding =>
          new File(s"$path.${encoding.extension}") -> encoding.encoding)
        .collectFirst {
          case (file, encoding) if file.exists =>
            respondWithHeader(`Content-Encoding`(encoding)) {
              getFromFile(file, contentType)
            }
        }
        .getOrElse(getFromFile(path))
  }

  private def safeJoinPaths(base: String,
                            path: Uri.Path,
                            log: LoggingAdapter,
                            separator: Char = File.separatorChar): String = {
    import java.lang.StringBuilder
    @tailrec def rec(p: Uri.Path,
                     result: StringBuilder = new StringBuilder(base)): String =
      p match {
        case Uri.Path.Empty       => result.toString
        case Uri.Path.Slash(tail) => rec(tail, result.append(separator))
        case Uri.Path.Segment(head, tail) =>
          if (head.indexOf('/') >= 0 || head.indexOf(
              '\\') >= 0 || head == "..") {
            log.warning(
              "File-system path for base [{}] and Uri.Path [{}] contains suspicious path segment [{}], " +
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
