package restui.models

import restui.protobuf.data.Schema

sealed trait Service {
  val id: String
  def toEvent: Event.Service
}

object Service {
  final case class OpenApi(id: String,
                           name: String,
                           file: String,
                           metadata: Map[String, String] = Map.empty,
                           useProxy: Boolean = false,
                           hash: String = "")
      extends Service {
    override def toEvent: Event.Service = Event.Service.OpenApi(id, name, useProxy, metadata)
  }

  final case class Grpc(id: String,
                        name: String,
                        schema: Schema,
                        servers: Map[String, Grpc.Server],
                        metadata: Map[String, String] = Map.empty)
      extends Service {
    override def toEvent: Event.Service = Event.Service.Grpc(id, name, servers.keys.toList, schema)
  }
  object Grpc {
    final case class Server(address: String, port: Int, useTls: Boolean)
  }
}
