package restui.models

sealed trait Service {
  val id: String
  def toEvent: Event.ServiceUp
}

object Service {
  final case class OpenApi(id: String,
                           name: String,
                           file: String,
                           metadata: Map[String, String] = Map.empty,
                           useProxy: Boolean = false,
                           hash: String = "")
      extends Service {
    override def toEvent: Event.ServiceUp = Event.ServiceUp(id, name, useProxy, metadata)
  }
}
