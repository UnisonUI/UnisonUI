package restui.server.http.templates

import akka.http.scaladsl.server.RequestContext

object Assets {

  val assetsDir = "/statics"

  def public(relativePath: String): String = s"$assetsDir/$relativePath"

  def style(style: Style): String = assetsDir + "/" + style.relativePath

  def relativeStyle(ctx: RequestContext, style: Style): String =
    (ctx.request.uri.path + "/" + style.relativePath).toString
}
