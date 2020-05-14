package restui.server.http.templates.css

import restui.server.http.templates.Style
import scalacss.DevDefaults._

object MainStyle extends StyleSheet.Standalone with Style {
  val name = "main"
  import dsl._

  "div.std" - (
    margin(12 px, auto),
    textAlign.left,
    cursor.pointer,
    &.hover -
      cursor.zoomIn,
    media.not.handheld.landscape.maxWidth(640 px) -
      width(400 px),
    &("span") -
      color.red
  )

  "h1".firstChild -
    fontWeight.bold

  for (i <- 0 to 3)
    s".indent-$i" -
      paddingLeft(i * 2.ex)
}
