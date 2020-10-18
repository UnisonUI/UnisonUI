package tech.unisonui.providers.docker.client.models

sealed trait State

object State {
  case object Start extends State
  case object Stop  extends State

  def fromString(string: String): Option[State] =
    string match {
      case "start" => Some(Start)
      case "stop"  => Some(Stop)
      case _       => None
    }
}
