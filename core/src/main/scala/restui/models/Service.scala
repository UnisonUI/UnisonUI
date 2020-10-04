package restui.models

import restui.protobuf.data.Schema

sealed trait Service {
  val id: String
  val hash: String
  def toEvent: Event.Service
}

object Service {
  private def computeSha1(input: String): String = {
    val md = java.security.MessageDigest.getInstance("SHA-1")
    md.digest(input.getBytes("UTF-8")).map("%02x".format(_)).mkString
  }

  final case class OpenApi(id: String, name: String, file: String, metadata: Map[String, String] = Map.empty, useProxy: Boolean = false)
      extends Service {
    override def toEvent: Event.Service = Event.Service.OpenApi(id, name, useProxy, metadata)
    override val hash: String           = computeSha1(file)
  }

  final case class Grpc(id: String,
                        name: String,
                        schema: Schema,
                        servers: Map[String, Grpc.Server],
                        metadata: Map[String, String] = Map.empty)
      extends Service {
    override def toEvent: Event.Service = Event.Service.Grpc(id, name, metadata)
    override val hash: String           = ""
  }
  object Grpc {
    final case class Server(address: String, port: Int, useTls: Boolean)
  }
}
