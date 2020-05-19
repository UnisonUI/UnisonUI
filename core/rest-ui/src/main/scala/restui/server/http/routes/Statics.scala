package restui.server.http.routes

import akka.http.scaladsl.coding.{Deflate, Gzip}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route

object Statics {
  val route: Route =
    encodeResponseWith(Gzip, Deflate) {
      pathPrefix("statics")(getFromResourceDirectory("web/statics")) ~
        path(PathEnd) {
          getFromResource("web/index.html")
        }
    }
}
