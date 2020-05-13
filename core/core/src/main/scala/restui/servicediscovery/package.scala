package restui.servicediscovery

object Models {
  final case class Endpoint(serviceName: String, address: String, port: String)
  sealed trait Event
  final case class Up(endpoint: Endpoint)   extends Event
  final case class Down(endpoint: Endpoint) extends Event
}
