package tech.unisonui.providers.docker.client.transport

import java.net.InetSocketAddress
import java.nio.file.Paths

import akka.actor.ActorSystem
import akka.http.scaladsl.settings.ClientConnectionSettings
import akka.http.scaladsl.{ClientTransport, Http}
import akka.stream.alpakka.unixdomainsocket.scaladsl.UnixDomainSocket
import akka.stream.scaladsl.Flow
import akka.util.ByteString

import scala.concurrent.Future

class DockerSock(file: String) extends ClientTransport {
  override def connectTo(host: String,
                         port: Int,
                         settings: ClientConnectionSettings)(implicit
      system: ActorSystem)
      : Flow[ByteString, ByteString, Future[Http.OutgoingConnection]] =
    UnixDomainSocket()
      .outgoingConnection(Paths.get(file))
      .mapMaterializedValue { _ =>
        Future.successful(
          Http.OutgoingConnection(
            InetSocketAddress.createUnresolved(host, port),
            InetSocketAddress.createUnresolved(host, port)))
      }
}
