package tech.unisonui.grpc

import java.{util => ju}

import akka.actor.ClassicActorSystemProvider
import akka.grpc.GrpcClientSettings
import akka.stream.scaladsl.{Sink, Source}
import io.circe.Json
import tech.unisonui.models.Service.Grpc.Server
import tech.unisonui.protobuf.data.{Schema, Service}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.chaining._

trait ReflectionClient {
  def loadSchema(server: Server): Future[Schema]
}

class ReflectionClientImpl(implicit sys: ClassicActorSystemProvider)
    extends ReflectionClient {
  implicit private val ec: ExecutionContext = sys.classicSystem.dispatcher
  private val reflectionService = Thread.currentThread.getContextClassLoader
    .getResourceAsStream("reflection.protoset")
    .readAllBytes
    .pipe(Schema.fromBytes)
    .map(_.services("grpc.reflection.v1alpha.ServerReflection"))

  private val listServicesRequest = Json.obj(
    "message_request" -> Json.obj("type" -> Json.fromString("list_services"),
                                  "value" -> Json.fromString("*")))

  private def listFilesRequest(symbol: String) = Json.obj(
    "message_request" -> Json.obj(
      "type"  -> Json.fromString("file_containing_symbol"),
      "value" -> Json.fromString(symbol)))

  override def loadSchema(from: Server): Future[Schema] =
    reflectionService.fold(
      Future.failed,
      service => {
        val client = createClient(service, from)

        listServicesRequest
          .pipe(doRequest(_, client))
          .flatMapConcat(retrieveFileDescriptorsFromServices(_, client))
          .mapConcat(extractFileDescriptorsFromJson)
          .watchTermination()((_, fut) => fut.flatMap(_ => client.close()))
          .runWith(Sink.seq)
          .flatMap {
            case Seq() => Future.failed(new Exception("No service(s) found"))
            case files =>
              files.toVector
                .pipe(Schema.fromFileDescriptorProtos)
                .fold(Future.failed, Future.successful)
          }
      }
    )

  private def createClient(service: Service, server: Server): Client = {
    val Server(address, port, useTls) = server
    val settings =
      GrpcClientSettings
        .connectToServiceAt(address, port)
        .withTls(useTls)
    new Client(service, settings)
  }

  private def retrieveFileDescriptorsFromServices(
      json: Json,
      client: Client): Source[Json, _] =
    json
      .pipe(extractServiceNamesFromJson)
      .foldLeft(Source.empty[Json]) { (source, service) =>
        source.merge(
          service
            .pipe(listFilesRequest)
            .pipe(doRequest(_, client)))
      }

  private def extractServiceNamesFromJson(json: Json): Vector[String] =
    json.hcursor
      .downField("message_response")
      .downField("value")
      .downField("service")
      .focus
      .toVector
      .flatMap(_.asArray.toVector.flatten.flatMap(
        _.hcursor.get[String]("name").toOption.toVector))
      .filterNot(_ == "grpc.reflection.v1alpha.ServerReflection")

  private def doRequest(request: Json, client: Client): Source[Json, _] = Source
    .single(request)
    .pipe(client.request("ServerReflectionInfo", _))
    .fold(Source.empty[Json])(identity(_))

  private def extractFileDescriptorsFromJson(json: Json): Vector[Array[Byte]] =
    json.hcursor
      .downField("message_response")
      .downField("value")
      .get[Vector[String]]("file_descriptor_proto")
      .toOption
      .toVector
      .flatten
      .map(_.pipe(ju.Base64.getDecoder().decode))
}
