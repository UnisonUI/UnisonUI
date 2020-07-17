package restui.server.http.routes

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import restui.server.http.directives.Encodings
import restui.server.http.directives.StaticsDirectives._
object Statics {
  val route: Route =
    pathPrefix("statics") {
      staticsFromResourceDirectory("web/statics", Encodings.Brotli, Encodings.Gzip)
    } ~ path(PathEnd) {
      getFromResource("web/index.html")
    }
}
