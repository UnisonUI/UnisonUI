package restui.server.http.routes

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.util.Timeout
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
import io.circe.syntax._
import io.circe.{Encoder, Json}
import restui.models.{Event, Service}
import restui.protobuf.data.Schema
import restui.server.service.ServiceActor
import restui.server.service.ServiceActor._

object Services {
  final case class GrpcResponse(schema: Schema, servers: Map[String, Service.Grpc.Server])
  implicit val encoder: Encoder[GrpcResponse] = (response: GrpcResponse) =>
    Json.obj(
      "servers" -> response.servers.map {
        case (name, Service.Grpc.Server(_, _, useTls)) => Json.obj("name" -> Json.fromString(name), "useTls" -> Json.fromBoolean(useTls))
      }.asJson,
      "schema" -> response.schema.asJson
    )
  implicit val timeout: Timeout = 5.seconds

  def route(
      serviceActorRef: ActorRef[ServiceActor.Message])(implicit actorSystem: ActorSystem[_], executionContext: ExecutionContext): Route =
    (path("services") & get) {
      val response =
        serviceActorRef
          .ask(GetAll(_))
          .mapTo[List[Service]]
          .map(_.map(service => Event.ServiceUp(service.toEvent)))
      complete(response)
    } ~ (path("services" / Remaining) & get) { service =>
      val response = serviceActorRef.ask(Get(_, service))
      onSuccess(response) {
        case Some(Service.OpenApi(_, _, content, _, _)) =>
          complete(StatusCodes.OK -> HttpEntity(ContentTypes.`text/plain(UTF-8)`, content))
        case Some(Service.Grpc(_, _, schema, servers, _)) =>
          complete(GrpcResponse(schema, servers))
        case _ => complete(StatusCodes.NotFound -> HttpEntity(ContentTypes.`text/plain(UTF-8)`, s"$service is not registered"))

      }
    }

}
