package restui.protobuf.data

import java.io.File
import java.nio.file.{Path, Paths}

import scala.util.control.Exception.allCatch

import cats.syntax.either._
import com.typesafe.scalalogging.LazyLogging
import io.circe.Json
import io.circe.parser.parse
import io.circe.syntax._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import restui.protobuf.ProtobufCompiler
import restui.protobuf.data.Schema._
import restui.protobuf.marshal.Reader._
import restui.protobuf.marshal.Writer._
class SchemaSpec extends AnyFlatSpec with Matchers with LazyLogging {
  implicit val compiler: ProtobufCompiler = new ProtobufCompiler {
    override def compile(path: Path): Either[Throwable, File] = new File(s"${path.toAbsolutePath().toString}set").asRight[Throwable]
    override def clean(file: File): Either[Throwable, Unit]   = ().asRight
  }
  it should "yes" in {
    // implicit val a = ActorSystem()
    // val s          = GrpcClientSettings.connectToServiceAt("127.0.0.1", 50051).withTls(false)
    for {
      schema <- Paths.get("src/test/resources/helloworld.proto").toSchema
      input = parse("""{"name":"test"}""").getOrElse(Json.Null)
      is    = schema.services("helloworld.Greeter").methods.head.inputType
      bytes <- allCatch.either(is.write(input))
      _ = logger.info(bytes.map("%02X" format _).mkString)
      decoded <- allCatch.either(is.read(bytes))
      _ = logger.info(decoded.spaces2)
      _ = logger.info(schema.asJson.spaces2)
      // r = Await.result(new Client(schema.services("helloworld.Greeter"), s).request("helloworld.Greeter.SayHello", input).get, Duration.Inf)
      // _ = logger.info(r.spaces2)
    } yield ()
  }
}
