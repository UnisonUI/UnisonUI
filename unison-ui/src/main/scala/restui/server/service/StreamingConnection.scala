package restui.server.service

import akka.NotUsed
import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.grpc.GrpcClientSettings
import akka.http.scaladsl.model.ws.{BinaryMessage, Message, TextMessage}
import akka.stream.ClosedShape
import akka.stream.scaladsl._
import akka.util.Timeout
import com.typesafe.scalalogging.LazyLogging
import io.circe.Json
import io.circe.parser.parse
import restui.grpc.Client
import restui.models.Service
import restui.protobuf.data.{Method, Service => ProtobufService}

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.util.chaining._

object StreamingConnection extends LazyLogging {
  implicit val timeout: Timeout = 5.seconds
  def start(serviceActorRef: ActorRef[ServiceActor.Message],
            id: String,
            service: String,
            method: String,
            server: String)(implicit
      actorSystem: ActorSystem[_],
      executionContext: ExecutionContext)
      : Future[Option[Flow[Message, Message, _]]] =
    serviceActorRef
      .ask(ServiceActor.Get(_, id))
      .map {
        _.collect {
          case Service.Grpc(_, _, schema, servers, _)
              if servers.contains(server) && schema.services.exists {
                case (currentService, ProtobufService(_, _, methods)) =>
                  currentService == service && methods.exists {
                    case Method(name, _, _, _, _) => name == method
                  }
              } =>
            val (sink, source)             = createSinkSource[Message]
            val (errorSink, errorSource)   = createSinkSource[Json]
            val (parsedSink, parsedSource) = createSinkSource[Json]

            RunnableGraph
              .fromGraph(GraphDSL.create() { implicit builder =>
                import GraphDSL.Implicits._
                val partition = builder.add(
                  Partition[Either[Throwable, Json]](
                    2,
                    either => if (either.isLeft) 0 else 1))
                source.flatMapConcat {
                  case tm: TextMessage =>
                    tm.textStream.map(parse)
                  case bm: BinaryMessage =>
                    bm.dataStream.runWith(Sink.ignore)
                    Source.empty
                } ~> partition.in
                partition
                  .out(0)
                  .map(e =>
                    Json
                      .obj("error" -> Json.fromString(
                        e.swap.map(_.getMessage).getOrElse("")))) ~> errorSink
                partition.out(1).map(_.getOrElse(Json.Null)) ~> parsedSink
                ClosedShape
              })
              .run()
            val Service.Grpc.Server(address, port, useTls) = servers(server)
            val settings =
              GrpcClientSettings
                .connectToServiceAt(address, port)
                .withTls(useTls)
            val client = new Client(schema.services(service), settings)
            client
              .request(method, parsedSource.mapMaterializedValue(_ => NotUsed))
              .map { out =>
                Flow
                  .fromSinkAndSourceCoupled(
                    sink,
                    out
                      .map(json => Json.obj("success" -> json))
                      .merge(errorSource
                        .mapMaterializedValue(_ => NotUsed))
                      .map(json => TextMessage(json.noSpaces))
                  )
                  .recover(
                    _.getMessage
                      .pipe(Json.fromString)
                      .pipe("error" -> _)
                      .pipe(Json.obj(_))
                      .noSpaces
                      .pipe(TextMessage(_)))
                  .watchTermination()((_, fut) =>
                    fut.flatMap(_ => client.close()))
              }
        }.flatten
      }

  private def createSinkSource[T](implicit
      actorSystem: ActorSystem[_]): (Sink[T, _], Source[T, _]) = Source
    .asSubscriber[T]
    .toMat(Sink.asPublisher[T](fanout = false))(Keep.both)
    .mapMaterializedValue { case (sub, pub) =>
      (Sink.fromSubscriber(sub), Source.fromPublisher(pub))
    }
    .run()

}
