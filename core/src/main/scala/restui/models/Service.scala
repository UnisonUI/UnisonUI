package restui.models

final case class Service(id: String,
                         name: String,
                         file: String,
                         metadata: Map[String, String] = Map.empty,
                         useProxy: Boolean = false,
                         hash: String = "")
