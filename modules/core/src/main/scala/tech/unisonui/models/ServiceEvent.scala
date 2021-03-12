package tech.unisonui.models

sealed trait ServiceEvent

object ServiceEvent {
  final case class ServiceUp(service: Service) extends ServiceEvent
  final case class ServiceDown(id: String)     extends ServiceEvent
}
