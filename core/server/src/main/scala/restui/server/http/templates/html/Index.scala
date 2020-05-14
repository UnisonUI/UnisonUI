package restui.server.http.templates.html

import restui.server.http.templates.Assets
import restui.server.http.templates.css.MainStyle
import scalatags.Text.all._

object Index {
  val template = html(
    head(
      link(rel := "stylesheet", src := Assets.style(MainStyle))
    ),
    body(
      h1("This is my title"),
      div(
        p("This is my first paragraph"),
        p("This is my second paragraph")
      )
    )
  )
}
