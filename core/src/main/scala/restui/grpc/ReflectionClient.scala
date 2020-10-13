package restui.grpc

import java.{util => ju}

import akka.actor.ClassicActorSystemProvider
import akka.grpc.GrpcClientSettings
import akka.stream.scaladsl.{Sink, Source}
import com.typesafe.scalalogging.LazyLogging
import io.circe.Json
import restui.models.Service
import restui.protobuf.data.Schema

import scala.concurrent.{ExecutionContext, Future}
import scala.util.chaining._

object ReflectionClient extends LazyLogging {
  private val reflectionService = Thread.currentThread.getContextClassLoader
    .getResourceAsStream("reflection.protoset")
    .readAllBytes
    .pipe(Schema.fromBytes)
    .map(_.services("grpc.reflection.v1alpha.ServerReflection"))

  private val listServicesRequest = Json.obj(
    "message_request" -> Json.obj("type" -> Json.fromString("list_services"),
                                  "value" -> Json.fromString("*")))

  private def listFiles(symbol: String) = Json.obj(
    "message_request" -> Json.obj(
      "type"  -> Json.fromString("file_containing_symbol"),
      "value" -> Json.fromString(symbol)))

  def loadSchema(server: Service.Grpc.Server)(implicit
      sys: ClassicActorSystemProvider,
      executionContext: ExecutionContext): Future[Schema] =
    reflectionService.fold(
      Future.failed,
      service => {
        val Service.Grpc.Server(address, port, useTls) = server
        val settings =
          GrpcClientSettings
            .connectToServiceAt(address, port)
            .withTls(useTls)
        val client = new Client(service, settings)
        Source
          .single(listServicesRequest)
          .pipe(client.streamingRequest("ServerReflectionInfo", _))
          .fold(Source.empty[Json])(identity(_))
          .flatMapConcat(json =>
            json.hcursor
              .downField("message_response")
              .downField("value")
              .downField("service")
              .focus
              .toVector
              .flatMap(_.asArray.toVector.flatten.flatMap(
                _.hcursor.get[String]("name").toOption.toVector))
              .filterNot(_ == "grpc.reflection.v1alpha.ServerReflection")
              .foldLeft(Source.empty[Json]) { (source, service) =>
                source.merge(
                  service
                    .pipe(listFiles)
                    .pipe(Source.single)
                    .pipe(client.streamingRequest("ServerReflectionInfo", _))
                    .fold(Source.empty[Json])(identity(_)))
              })
          .mapConcat { json =>
            json.hcursor
              .downField("message_response")
              .downField("value")
              .get[Vector[String]]("file_descriptor_proto")
              .toOption
              .toVector
              .flatten
              .map(_.pipe(ju.Base64.getDecoder().decode))
          }
          .watchTermination()((_, fut) => fut.flatMap(_ => client.close()))
          .runWith(Sink.seq)
          .flatMap(_.toVector
            .pipe(Schema.fromFileDescriptorProtos)
            .fold(Future.failed, Future.successful))
      }
    )
}
