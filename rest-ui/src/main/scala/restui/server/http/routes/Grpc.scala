package restui.server.http.routes

import java.nio.charset.StandardCharsets
import java.{util => ju}

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.util.chaining._

import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.grpc.GrpcClientSettings
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.util.Timeout
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
import io.circe.Json
import io.circe.generic.auto._
import restui.grpc.Client
import restui.models.Service
import restui.protobuf.data.{Method, Service => ProtobufService}
import restui.server.service.ServiceActor

object Grpc {
  final case class Input(server: String, data: Json)
  implicit val timeout: Timeout = 5.seconds
  def route(
      serviceActorRef: ActorRef[ServiceActor.Message])(implicit actorSystem: ActorSystem[_], executionContext: ExecutionContext): Route =
    (path("grpc" / Segment / Segment / Segment) & post & entity(as[Input])) { (base64Id, service, method, input) =>
      val id = decode(base64Id)
      val response: Future[Option[Json]] = serviceActorRef
        .ask(ServiceActor.Get(_, id))
        .flatMap {
          case Some(Service.Grpc(_, _, schema, servers, _)) if servers.contains(input.server) && schema.services.exists {
                case (currentService, ProtobufService(_, _, methods)) =>
                  currentService == service && methods.exists { case Method(name, _, _) => name == method }
              } =>
            val Service.Grpc.Server(address, port, useTls) = servers(input.server)
            val settings                                   = GrpcClientSettings.connectToServiceAt(address, port).withTls(useTls)
            val client                                     = new Client(schema.services(service), settings)
            client.request(method, input.data).fold(Future.successful(Option.empty[Json])) {
              _.map { result =>
                client.close()
                Some(result)
              }

            }
          case None => Future.successful(None)

        }
      onSuccess(response) {
        case Some(json) =>
          complete(json)
        case None =>
          complete(StatusCodes.NotFound -> HttpEntity(ContentTypes.`text/plain(UTF-8)`, s"$id is not registered"))
      }
    }
  private def decode(input: String): String =
    new String(input.replaceAll("_", "/").getBytes(StandardCharsets.UTF_8).pipe(ju.Base64.getDecoder.decode))
}
