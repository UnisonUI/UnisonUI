package restui.providers.docker.client.models

sealed trait State

object State {
  case object Start extends State
  case object Stop  extends State
  case object Kill  extends State

  def fromString(string: String): Option[State] =
    string match {
      case "start" => Some(Start)
      case "stop"  => Some(Stop)
      case "kill"  => Some(Kill)
      case _       => None
    }
}
