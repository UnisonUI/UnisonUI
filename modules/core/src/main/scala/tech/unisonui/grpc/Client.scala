package tech.unisonui.grpc

import akka.actor.ClassicActorSystemProvider
import akka.grpc.GrpcClientSettings
import akka.grpc.internal.{
  ClientState,
  NettyClientUtils,
  ScalaBidirectionalStreamingRequestBuilder,
  ScalaClientStreamingRequestBuilder,
  ScalaServerStreamingRequestBuilder,
  ScalaUnaryRequestBuilder
}
import akka.stream.scaladsl.Source
import akka.{Done, NotUsed}
import io.circe.Json
import io.grpc.MethodDescriptor
import tech.unisonui.protobuf.data._

import scala.concurrent.{ExecutionContext, Future}
import scala.util.chaining._

object Client {
  type FutureJson = Future[Json]
  type SourceJson = Source[Json, NotUsed]
}

class Client(service: Service, settings: GrpcClientSettings)(implicit
    sys: ClassicActorSystemProvider) {
  import Client._
  private implicit val executionContext: ExecutionContext =
    sys.classicSystem.dispatcher
  private val clientState = new ClientState(
    settings,
    akka.event.Logging(sys.classicSystem, this.getClass))
  private val options = NettyClientUtils.callOptions(settings)

  def request(methodName: String, input: Json): Option[SourceJson] =
    input.pipe(Source.single).pipe(request(methodName, _))

  def request(methodName: String, source: SourceJson): Option[SourceJson] =
    service.methods.collectFirst {
      case method @ Method(name, _, _, true, true) if name == methodName =>
        val descriptor =
          methodDescriptor(MethodDescriptor.MethodType.BIDI_STREAMING, method)
        new ScalaBidirectionalStreamingRequestBuilder(
          descriptor,
          clientState.internalChannel,
          options,
          settings)
          .invoke(source)
      // $COVERAGE-OFF$
      case method @ Method(name, _, _, true, false) if name == methodName =>
        val descriptor =
          methodDescriptor(MethodDescriptor.MethodType.SERVER_STREAMING, method)
        val builder =
          new ScalaServerStreamingRequestBuilder(descriptor,
                                                 clientState.internalChannel,
                                                 options,
                                                 settings)
        source.flatMapConcat(builder.invoke(_))
      case method @ Method(name, _, _, false, true) if name == methodName =>
        val descriptor =
          methodDescriptor(MethodDescriptor.MethodType.CLIENT_STREAMING, method)
        val builder =
          new ScalaClientStreamingRequestBuilder(descriptor,
                                                 clientState.internalChannel,
                                                 options,
                                                 settings)
        Source.future(builder.invoke(source))
      case method @ Method(name, _, _, false, false) if name == methodName =>
        val descriptor =
          methodDescriptor(MethodDescriptor.MethodType.UNARY, method)
        val builder =
          new ScalaUnaryRequestBuilder(descriptor,
                                       clientState.internalChannel,
                                       options,
                                       settings)
        source
          .flatMapConcat(input => Source.future(builder.invoke(input)))
      // $COVERAGE-ON$
    }

  def close(): Future[Done] = clientState.close()

  private def methodDescriptor(`type`: MethodDescriptor.MethodType,
                               method: Method): MethodDescriptor[Json, Json] =
    MethodDescriptor
      .newBuilder()
      .setType(`type`)
      .setFullMethodName(
        MethodDescriptor.generateFullMethodName(service.fullName, method.name))
      .setRequestMarshaller(new Marshaller(method.inputType))
      .setResponseMarshaller(new Marshaller(method.outputType))
      .setSampledToLocalTracing(true)
      .build()
}
