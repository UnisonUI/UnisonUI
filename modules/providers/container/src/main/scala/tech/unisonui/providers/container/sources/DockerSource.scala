package tech.unisonui.providers.container.sources

import akka.NotUsed
import akka.actor.typed.ActorSystem
import akka.http.scaladsl.model.Uri
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.scaladsl.{JsonFraming, Merge, Source}
import com.typesafe.scalalogging.LazyLogging
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
import io.circe.parser.parse
import tech.unisonui.Concurrency
import tech.unisonui.grpc.ReflectionClient
import tech.unisonui.models.{Metadata, Service, ServiceEvent}
import tech.unisonui.providers.container.docker.client.HttpClient
import tech.unisonui.providers.container.docker.client.models.{
  Container,
  Event,
  State
}
import tech.unisonui.providers.container.settings._

import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.Exception.allCatch

class DockerSource(private val client: HttpClient,
                   private val reflectionClient: ReflectionClient,
                   private val settings: Settings)(implicit
    val system: ActorSystem[_])
    extends LazyLogging {
  import DockerSource._
  implicit val executionContent: ExecutionContext = system.executionContext

  def startStreaming: Source[ServiceEvent, NotUsed] =
    events.collect {
      case event @ Event(id, Some(state), attributes)
          if findMatchingLabels(attributes).isDefined =>
        logger.debug(s"Event found: $event")
        (id, state)
    }.flatMapMerge(
      Concurrency.AvailableCore,
      {
        case (id, State.Start) =>
          handleServiceUp(id).async
        case (id, _) =>
          Source.single(ServiceEvent.ServiceDown(id))
      }
    ).async

  private def events: Source[Event, NotUsed] =
    client
      .watch(Uri("/events").withRawQueryString(
        """since=0&filters={"event":["start","stop"],"type":["container"]}"""))
      .flatMapMerge(
        Concurrency.AvailableCore,
        response =>
          if (response.status.isSuccess) response.entity.dataBytes
          else
            Source.futureSource(
              response.entity.discardBytes().future.map(_ => Source.empty))
      )
      .via(JsonFraming.objectScanner(MaximumFrameSize))
      .flatMapMerge(
        Concurrency.AvailableCore,
        entity =>
          parse(entity.utf8String).flatMap(_.as[Event]) match {
            case Left(e) =>
              logger.warn("Decoding error", e)
              Source.empty
            case Right(event) => Source.single(event)
          }
      )

  private def handleServiceUp(id: String): Source[ServiceEvent, NotUsed] =
    container(id).async.flatMapConcat { container =>
      findEndpoint(container.labels, container.ip).fold(
        Source.empty[ServiceEvent]) {
        case ServiceNameWithAddress(serviceName, openApi, grpcServer) =>
          Source.combine(
            downloadFile(id, serviceName, openApi).async,
            loadSchema(id, serviceName, grpcServer).async)(Merge(_))
      }
    }

  private def container(id: String): Source[Container, NotUsed] =
    Source.futureSource {
      client
        .get(Uri(s"/containers/$id/json"))
        .flatMap { response =>
          if (response.status.isSuccess) Unmarshal(response).to[Container]
          else
            response.entity
              .discardBytes()
              .future
              .flatMap(_ =>
                Future.failed(new Exception(response.status.defaultMessage)))
        }
        .map(Source.single)
        .recover { throwable =>
          logger.warn(
            "There was an error while retrieving the container information",
            throwable)
          Source.empty[Container]
        }
    }.mapMaterializedValue(_ => NotUsed)

  private def downloadFile(
      id: String,
      serviceName: String,
      maybeOpenApi: Option[OpenApiService]): Source[ServiceEvent, NotUsed] =
    maybeOpenApi.fold(Source.empty[ServiceEvent]) {
      case OpenApiService(endpoint, useProxy) =>
        Source.futureSource {
          client
            .downloadFile(endpoint)
            .map { content =>
              val metadata = Map(
                Metadata.Provider -> "container",
                Metadata.File     -> Uri(endpoint).path.toString.substring(1)
              )

              Source.single(
                ServiceEvent.ServiceUp(
                  Service.OpenApi(id,
                                  serviceName,
                                  content,
                                  metadata,
                                  useProxy = useProxy)
                )
              )
            }
            .recover { throwable =>
              logger.warn("There was an error while download the file",
                          throwable)
              Source.empty[ServiceEvent]
            }
        }.mapMaterializedValue(_ => NotUsed)
    }

  private def loadSchema(
      id: String,
      serviceName: String,
      grpcServer: Option[Service.Grpc.Server]): Source[ServiceEvent, NotUsed] =
    grpcServer.fold(Source.empty[ServiceEvent]) { server =>
      val address = s"${server.address}:${server.port}"
      Source.futureSource {
        reflectionClient
          .loadSchema(server)
          .map { schema =>
            val metadata = Map(
              Metadata.Provider -> "container",
              Metadata.File     -> address
            )
            Source.single(
              ServiceEvent.ServiceUp(
                Service.Grpc(id,
                             serviceName,
                             schema,
                             Map(address -> server),
                             metadata)
              )
            )
          }
          .recover { throwable =>
            logger.warn("There was an error while retrieving the schema",
                        throwable)
            Source.empty[ServiceEvent]
          }
      }.mapMaterializedValue(_ => NotUsed)
    }

  private def findEndpoint(
      labels: Map[String, String],
      maybeIpAddress: Option[String]): Option[ServiceNameWithAddress] =
    for {
      labels    <- findMatchingLabels(labels)
      ipAddress <- maybeIpAddress
      openApi = labels.openapi.map {
        case OpenApiLabels(port, protocol, specificationPath, useProxy) =>
          val endpoint =
            s"$protocol://$ipAddress:${port.toInt}${specificationPath}"
          val useProxyValue = allCatch.opt(useProxy.toBoolean).getOrElse(false)
          OpenApiService(endpoint, useProxyValue)
      }
      grpcServer = labels.grpc.map { case GrpcLabels(port, tls) =>
        val tlsValue = allCatch.opt(tls.toBoolean).getOrElse(false)
        Service.Grpc.Server(ipAddress, port.toInt, tlsValue)
      }
    } yield ServiceNameWithAddress(labels.serviceName, openApi, grpcServer)

  private def findMatchingLabels(labels: Map[String, String]): Option[Labels] =
    for {
      serviceName <- labels.get(settings.labels.serviceName)
      newLabels   <- settings.labels.extractLabels(labels)
    } yield newLabels.copy(serviceName = serviceName)
}

object DockerSource {
  private final case class OpenApiService(endpoint: String, useProxy: Boolean)
  private final case class ServiceNameWithAddress(
      serviceName: String,
      openApi: Option[OpenApiService],
      grpcPort: Option[Service.Grpc.Server])
  private val MaximumFrameSize: Int = 10000
}
