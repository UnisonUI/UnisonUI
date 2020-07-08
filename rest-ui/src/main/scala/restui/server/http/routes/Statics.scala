package restui.server.http.routes

import scala.annotation.tailrec

import akka.event.LoggingAdapter
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.{`Accept-Encoding`, `Content-Encoding`, HttpEncoding, HttpEncodings, Range}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route

object Statics {
  private val classLoader    = Statics.getClass.getClassLoader
  private val BrotliEncoding = HttpEncoding.custom("br")

  val route: Route =
    extractRequest { request =>
      pathPrefix("statics") {
        val base = withTrailingSlash("web/statics")
        extractUnmatchedPath { path =>
          extractLog { log =>
            val isRange = request.header[Range].isDefined
            val (isBrotliEncoding, isGzip) = request
              .header[`Accept-Encoding`]
              .map(encoding => (isAcceptEncoding(encoding, BrotliEncoding), isAcceptEncoding(encoding, HttpEncodings.gzip)))
              .getOrElse((false, false))
            safeJoinPaths(base, path, log, separator = '/') match {
              case "" => reject
              case resourceName =>
                serveFile(resourceName, isRange, isBrotliEncoding, isGzip)
            }
          }
        }
      }
    } ~ path(PathEnd) {
      getFromResource("web/index.html")
    }

  private def withTrailingSlash(path: String): String = if (path endsWith "/") path else path + '/'

  private def serveFile(resourceName: String, isRange: Boolean, isBrotli: Boolean, isGzip: Boolean): Route = {
    val contentType = ContentType(MediaTypes.forExtension(resourceName.split('.').last), () => HttpCharsets.`UTF-8`)
    (isRange, isBrotli, isGzip) match {
      case (false, true, _) if Option(classLoader.getResource(s"$resourceName.br")).isDefined =>
        respondWithHeader(`Content-Encoding`(BrotliEncoding)) {
          getFromResource(s"$resourceName.br", contentType, classLoader)
        }
      case (false, _, true) if Option(classLoader.getResource(s"$resourceName.gz")).isDefined =>
        respondWithHeader(`Content-Encoding`(HttpEncodings.gzip)) {
          getFromResource(s"$resourceName.gz", contentType, classLoader)
        }
      case (_, _, _) =>
        getFromResource(resourceName)
    }
  }

  private def isAcceptEncoding(acceptEncoding: `Accept-Encoding`, encoding: HttpEncoding): Boolean =
    acceptEncoding.encodings.exists(_.matches(encoding))

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
