package restui.models

sealed trait ContentType

object ContentType {

  case object Plain extends ContentType
  case object Json  extends ContentType
  case object Yaml  extends ContentType

  def fromString(string: String): ContentType =
    if (string.endsWith(".yaml") || string.endsWith(".yml")) Yaml
    else if (string.endsWith(".json")) Json
    else Plain
}
