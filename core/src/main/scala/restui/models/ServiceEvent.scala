package restui.models

sealed trait ServiceEvent

object ServiceEvent {
  final case class ServiceUp(service: Service)      extends ServiceEvent
  final case class ServiceDown(serviceName: String) extends ServiceEvent
}
