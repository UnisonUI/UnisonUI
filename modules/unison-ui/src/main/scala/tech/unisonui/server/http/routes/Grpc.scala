package tech.unisonui.server.http.routes

import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.grpc.GrpcClientSettings
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.scaladsl.Sink
import akka.util.Timeout
import cats.syntax.either._
import cats.syntax.option._
import com.typesafe.scalalogging.LazyLogging
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
import io.circe.generic.auto._
import io.circe.{DecodingFailure, Json}
import tech.unisonui.grpc.Client
import tech.unisonui.models.Service
import tech.unisonui.protobuf.data.{Method, Service => ProtobufService}
import tech.unisonui.server.http.Base64
import tech.unisonui.server.service.ServiceActor.Message
import tech.unisonui.server.service.{ServiceActor, StreamingConnection}

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

object Grpc extends LazyLogging {
  final case class Input(server: String, data: Json)
  implicit val timeout: Timeout = 5.seconds

  def route(serviceActorRef: ActorRef[Message])(implicit
      actorSystem: ActorSystem[_],
      executionContext: ExecutionContext): Route =
    (path("grpc" / "streaming" / Segment / Segment / Segment) & parameter(
      "server") & get) { (base64Id, service, method, server) =>
      val id = Base64.decode(base64Id)
      val response = StreamingConnection
        .start(serviceActorRef, id, service, method, server)
        .map(_.asRight[Throwable])
        .recover(_.asLeft)
      onSuccess(response) {
        case Right(None) =>
          complete(
            StatusCodes.NotFound -> HttpEntity(ContentTypes.`text/plain(UTF-8)`,
                                               s"$id is not registered"))
        case Right(Some(flow)) => handleWebSocketMessages(flow)
        case Left(exception) =>
          logger.warn("{}", exception)
          complete(
            StatusCodes.InternalServerError -> HttpEntity(
              ContentTypes.`text/plain(UTF-8)`,
              s"Grpc client error: ${exception.getMessage()}"))
      }
    } ~
      (path("grpc" / Segment / Segment / Segment) & post & entity(as[Input])) {
        (base64Id, service, method, input) =>
          val id = Base64.decode(base64Id)
          val response: Future[Either[Throwable, Option[Json]]] =
            serviceActorRef
              .ask(ServiceActor.Get(_, id))
              .flatMap {
                case Some(Service.Grpc(_, _, schema, servers, _))
                    if servers.contains(
                      input.server) && schema.services.exists {
                      case (currentService, ProtobufService(_, _, methods)) =>
                        currentService == service && methods.exists {
                          case Method(name, _, _, _, _) => name == method
                        }
                    } =>
                  grpcCall(input.data,
                           method,
                           schema.services(service),
                           servers(input.server))
                case None => Future.successful(None.asRight)

              }

          onSuccess(response.recover(_.asLeft)) {
            case Right(Some(json)) =>
              complete(json)
            case Right(None) =>
              complete(
                StatusCodes.NotFound -> HttpEntity(
                  ContentTypes.`text/plain(UTF-8)`,
                  s"$id is not registered"))
            case Left(exception: DecodingFailure) =>
              complete(
                StatusCodes.BadRequest -> HttpEntity(
                  ContentTypes.`text/plain(UTF-8)`,
                  exception.getMessage()))
            case Left(exception) =>
              complete(
                StatusCodes.InternalServerError -> HttpEntity(
                  ContentTypes.`text/plain(UTF-8)`,
                  s"Grpc client error: ${exception.getMessage()}"))
          }
      }

  private def grpcCall(input: Json,
                       method: String,
                       service: ProtobufService,
                       server: Service.Grpc.Server)(implicit
      actorSystem: ActorSystem[_],
      executionContext: ExecutionContext)
      : Future[Either[Throwable, Option[Json]]] = {
    val Service.Grpc.Server(address, port, useTls) = server
    val settings =
      GrpcClientSettings.connectToServiceAt(address, port).withTls(useTls)
    val client = new Client(service, settings)
    client
      .request(method, input)
      .fold(Future.successful(Option.empty[Json].asRight[Throwable])) {
        _.runWith(Sink.head).map { result =>
          client.close()
          result.some.asRight[Throwable]
        }
      }
  }
}
