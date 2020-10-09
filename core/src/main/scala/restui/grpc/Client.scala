package restui.grpc

import akka.Done
import akka.actor.ClassicActorSystemProvider
import akka.grpc.GrpcClientSettings
import akka.grpc.internal.{
  ClientState,
  NettyClientUtils,
  ScalaUnaryRequestBuilder
}
import io.circe.Json
import io.grpc.MethodDescriptor
import restui.protobuf.data._

import scala.concurrent.{ExecutionContext, Future}

class Client(service: Service, settings: GrpcClientSettings)(implicit
    sys: ClassicActorSystemProvider) {
  private implicit val executionContext: ExecutionContext =
    sys.classicSystem.dispatcher
  private val clientState = new ClientState(
    settings,
    akka.event.Logging(sys.classicSystem, this.getClass))
  private val options = NettyClientUtils.callOptions(settings)

  def request(methodName: String, input: Json): Option[Future[Json]] =
    service.methods.find(m => m.name == methodName).map { method =>
      val descriptor = methodDescriptor(method)
      new ScalaUnaryRequestBuilder(descriptor,
                                   clientState.internalChannel,
                                   options,
                                   settings).invoke(input)
    }

  def close(): Future[Done] = clientState.close()

  private def methodDescriptor(method: Method): MethodDescriptor[Json, Json] =
    MethodDescriptor
      .newBuilder()
      .setType(MethodDescriptor.MethodType.UNARY)
      .setFullMethodName(
        MethodDescriptor.generateFullMethodName(service.fullName, method.name))
      .setRequestMarshaller(new Marshaller(method.inputType))
      .setResponseMarshaller(new Marshaller(method.outputType))
      .setSampledToLocalTracing(true)
      .build()
}
