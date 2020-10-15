package restui.providers.docker

import akka.NotUsed
import akka.actor.typed.ActorSystem
import akka.http.scaladsl.model.Uri
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.scaladsl.{JsonFraming, Merge, Source}
import com.typesafe.scalalogging.LazyLogging
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
import io.circe.parser.parse
import restui.Concurrency
import restui.grpc.ReflectionClient
import restui.models.{Metadata, Service, ServiceEvent}
import restui.providers.docker.client.HttpClient
import restui.providers.docker.client.models.{Container, Event, State}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class DockerClient(private val client: HttpClient,
                   private val reflectionClient: ReflectionClient,
                   private val settings: Settings)(implicit
    val system: ActorSystem[_])
    extends LazyLogging {
  import DockerClient._
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
        case ServiceNameWithAddress(serviceName,
                                    address,
                                    useProxy,
                                    grpcServer) =>
          Source.combine(
            downloadFile(id, serviceName, address, useProxy).async,
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

  private def downloadFile(id: String,
                           serviceName: String,
                           maybeUri: Option[String],
                           useProxy: Boolean): Source[ServiceEvent, NotUsed] =
    maybeUri.fold(Source.empty[ServiceEvent]) { uri =>
      Source.futureSource {
        client
          .downloadFile(uri)
          .map { content =>
            val metadata = Map(
              Metadata.Provider -> "docker",
              Metadata.File     -> Uri(uri).path.toString.substring(1)
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
            logger.warn("There was an error while download the file", throwable)
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
              Metadata.Provider -> "docker",
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
      specificationPort = Try(labels.port.toInt).toOption.flatMap {
        case 0    => None
        case port => Some(port)
      }
      endpoint = specificationPort.map(port =>
        s"http://$ipAddress:${port}${labels.specificationPath}")
      useProxy   = Try(labels.useProxy.toBoolean).getOrElse(false)
      grpcPort   = labels.grpcPort.flatMap(v => Try(v.toInt).toOption)
      grpcTls    = Try(labels.useProxy.toBoolean).getOrElse(false)
      grpcServer = grpcPort.map(Service.Grpc.Server(ipAddress, _, grpcTls))
    } yield
      ServiceNameWithAddress(labels.serviceName, endpoint, useProxy, grpcServer)

  private def findMatchingLabels(labels: Map[String, String]): Option[Labels] =
    for {
      serviceName <- labels.get(settings.labels.serviceName)
      port     = labels.getOrElse(settings.labels.port, "0")
      useProxy = labels.getOrElse(settings.labels.useProxy, "false")
      specificationPath = labels.getOrElse(settings.labels.specificationPath,
                                           "/specification.yaml")
      grpcPort = settings.labels.grpcPort.flatMap(labels.get(_))
      grpcTls  = labels.getOrElse(settings.labels.grpcTls, "false")
    } yield
      Labels(serviceName, port, specificationPath, useProxy, grpcPort, grpcTls)
}

object DockerClient {
  private final case class ServiceNameWithAddress(
      serviceName: String,
      address: Option[String],
      useProxy: Boolean,
      grpcPort: Option[Service.Grpc.Server])
  private val MaximumFrameSize: Int = 10000
}
