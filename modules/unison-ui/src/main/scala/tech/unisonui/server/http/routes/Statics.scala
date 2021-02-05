package tech.unisonui.server.http.routes

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import tech.unisonui.server.http.directives.Encodings
import tech.unisonui.server.http.directives.StaticsDirectives._
object Statics {
  def route(staticsPath: String): Route =
    pathPrefix("statics") {
      staticsFromDirectory(staticsPath, Encodings.Brotli, Encodings.Gzip)
    } ~ path(PathEnd) {
      getFromFile(s"$staticsPath/index.html")
    }
}
