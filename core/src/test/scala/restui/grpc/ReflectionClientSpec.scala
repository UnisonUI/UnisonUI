package restui.grpc

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import cats.syntax.option._
import com.google.protobuf.Descriptors.FieldDescriptor
import com.typesafe.config.ConfigFactory
import org.scalatest.flatspec.AsyncFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.{BeforeAndAfterAll, Inside}
import restui.GreeterServer
import restui.models.Service.Grpc.Server
import restui.protobuf.data._

import scala.concurrent.ExecutionContext

class ReflectionSpec
    extends AsyncFlatSpec
    with Matchers
    with Inside
    with BeforeAndAfterAll {
  val conf = ConfigFactory
    .parseString("akka.http.server.preview.enable-http2 = on")
    .withFallback(ConfigFactory.defaultApplication())
  implicit val system =
    ActorSystem[Nothing](Behaviors.empty, "GreeterServer", conf)
  implicit val ec: ExecutionContext = system.executionContext

  override protected def beforeAll(): Unit =
    GreeterServer.run(9000)
  override protected def afterAll(): Unit = system.terminate()
  private val expectedSchema = Schema(
    Map(
      "helloworld.HelloRequest" -> MessageSchema(
        "helloworld.HelloRequest",
        Map(
          1 -> Field(1,
                     "name",
                     Label.Optional,
                     FieldDescriptor.Type.STRING,
                     packed = false))),
      "helloworld.HelloReply" -> MessageSchema(
        "helloworld.HelloReply",
        Map(
          1 -> Field(1,
                     "message",
                     Label.Optional,
                     FieldDescriptor.Type.STRING,
                     packed = false)))
    )
  )

  it should "retrieve the schema using the server reflection" in {
    new ReflectionClientImpl()
      .loadSchema(Server("127.0.0.1", 9000, false))
      .map {
        _ shouldBe expectedSchema
          .copy(services = Map("helloworld.Greeter" -> Service(
            "Greeter",
            "helloworld.Greeter",
            Vector(
              Method(
                "SayHello",
                expectedSchema.copy(rootKey = "helloworld.HelloRequest".some),
                expectedSchema.copy(rootKey = "helloworld.HelloReply".some),
                isServerStreaming = false,
                isClientStreaming = false
              ),
              Method(
                "SayHelloToAll",
                expectedSchema.copy(rootKey = "helloworld.HelloRequest".some),
                expectedSchema.copy(rootKey = "helloworld.HelloReply".some),
                isServerStreaming = true,
                isClientStreaming = true
              )
            )
          )))
      }
  }
}
