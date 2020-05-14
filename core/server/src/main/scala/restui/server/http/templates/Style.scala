package restui.server.http.templates

trait Style { this: scalacss.internal.mutable.StyleSheet.Base =>
  def name: String
  def relativePath: String = "css/" + name + ".css"
}
