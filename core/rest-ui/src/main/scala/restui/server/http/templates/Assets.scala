package restui.server.http.templates

object Assets {

  val assetsDir = "/statics"

  def public(relativePath: String): String = s"$assetsDir/$relativePath"

}
