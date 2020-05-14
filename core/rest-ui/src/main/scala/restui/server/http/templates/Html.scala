package restui.server.http.templates

import scalatags.Text.all._
import scalatags.Text.tags2.{title => titleTag, style => styleTag}
import akka.http.scaladsl.server.RequestContext
import java.nio.charset.StandardCharsets

object Html {
  private def css(path: String) = link(rel := "stylesheet", media := "screen", href := path, `type` := "text/css")
  private def icon(iconSize: Int) =
    link(rel := "icon",
         `type` := "image/png",
         href := Assets.public(s"favicon-${iconSize}x$iconSize.png"),
         size := s"${iconSize}x$iconSize")

  def template(implicit context: RequestContext) =
    html(
      lang := "en",
      head(
        meta(charset := StandardCharsets.UTF_8.name.toLowerCase),
        css(Assets.public("swagger-ui.css")),
        titleTag("RestUI"),
        icon(32),
        icon(16),
        styleTag("""|
|html
|{
|  box-sizing: border-box;
|  overflow: -moz-scrollbars-vertical;
|  overflow-y: scroll;
|}
|
|*,
|*:before,
|*:after
|{
|  box-sizing: inherit;
|}
|
|body
|{
|  margin:0;
|  background: #fafafa;
|}
""".stripMargin)
      ),
      body(
        div(id := "swagger-ui"),
        script(src := Assets.public("swagger-ui-bundle.js")),
        script(src := Assets.public("swagger-ui-standalone-preset.js")),
        raw("""|<script>
|    window.onload = function() {
|      // Begin Swagger UI call region
|      const ui = SwaggerUIBundle({
|        url: "https://petstore.swagger.io/v2/swagger.json",
|        dom_id: '#swagger-ui',
|        deepLinking: true,
|        presets: [
|          SwaggerUIBundle.presets.apis,
|        ],
|        plugins: [],
|        layout: "BaseLayout"
|      })
|      // End Swagger UI call region
|
|      window.ui = ui
|
| fetch(`${window.location.protocol}//${window.location.host}/endpoints`)
| .then(response => response.json())
| .then(data => console.log(data));
| }
| </script>
""".stripMargin)
      )
    )
}
