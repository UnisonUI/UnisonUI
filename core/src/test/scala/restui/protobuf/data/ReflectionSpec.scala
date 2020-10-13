package restui.grpc

import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import com.typesafe.scalalogging.LazyLogging
import io.circe.syntax._
import org.scalatest.Inside
import org.scalatest.flatspec.AnyFlatSpecLike
import org.scalatest.matchers.should.Matchers
import restui.models.Service

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext}
import scala.util.chaining._

class ReflectionSpec
    extends ScalaTestWithActorTestKit
    with AnyFlatSpecLike
    with Matchers
    with Inside
    with LazyLogging {
  implicit val ec: ExecutionContext = system.executionContext
  it should "lol" in {
    ReflectionClient
      .loadSchema(Service.Grpc.Server("127.0.0.1", 9000, false))
      .map(_.asJson)
      .pipe(Await.result(_, Duration.Inf))
      .pipe(j => logger.info(j.spaces2))
  }

}
