package tech.unisonui

import akka.NotUsed
import akka.actor.typed.ActorSystem
import akka.grpc.scaladsl.{ServerReflection, ServiceHandler}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.stream.scaladsl.{BroadcastHub, Keep, MergeHub, Sink, Source}
import com.example.helloworld._

import scala.concurrent.Future

object GreeterServer {

  class GreeterImpl(implicit val system: ActorSystem[_]) extends Greeter {
    val (inboundHub: Sink[HelloRequest, NotUsed],
         outboundHub: Source[HelloReply, NotUsed]) =
      MergeHub
        .source[HelloRequest]
        .map(request => HelloReply(s"Hello, ${request.name}"))
        .toMat(BroadcastHub.sink[HelloReply])(Keep.both)
        .run()

    override def sayHello(request: HelloRequest): Future[HelloReply] =
      Future.successful(HelloReply(s"Hello, ${request.name}"))

    override def sayHelloToAll(
        in: Source[HelloRequest, NotUsed]): Source[HelloReply, NotUsed] = {
      in.runWith(inboundHub)
      outboundHub
    }
  }
  def run(port: Int = 9000)(implicit
      system: ActorSystem[_]): Future[Http.ServerBinding] = {

    val service: PartialFunction[HttpRequest, Future[HttpResponse]] =
      GreeterHandler.partial(new GreeterImpl)
    val reflection: PartialFunction[HttpRequest, Future[HttpResponse]] =
      ServerReflection.partial(List(Greeter))

    Http()
      .newServerAt("127.0.0.1", port)
      .bind(ServiceHandler.concatOrNotFound(service, reflection))
  }
}
