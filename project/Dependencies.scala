import sbt.Keys._
import sbt._

object Dependencies {

  object Config {
    private val config = "com.typesafe" % "config" % "1.4.1"
    val all            = Seq(config)
  }

  object Logging {
    private val logback     = "ch.qos.logback" % "logback-classic" % "1.2.3"
    private val logbackCore = "ch.qos.logback" % "logback-core"    % "1.2.3"
    private val logstash =
      "net.logstash.logback" % "logstash-logback-encoder" % "6.6"
    private val scalaLogging =
      "com.typesafe.scala-logging" %% "scala-logging" % "3.9.3"
    val all = Seq(logback, logbackCore, logstash, scalaLogging)
  }

  object Circe {
    private val version = "0.13.0"
    private val core    = "io.circe" %% "circe-core"        % version
    private val generic = "io.circe" %% "circe-generic"     % version
    private val yaml    = "io.circe" %% "circe-yaml"        % "0.13.1"
    val parser          = "io.circe" %% "circe-parser"      % version
    val schema          = "io.circe" %% "circe-json-schema" % "0.2.0"
    val all             = Seq(core, generic, yaml)
  }

  object Grpc {
    private val grpcVersion     = "1.36.0"
    private val protobufVersion = "3.15.6"
    private val protobuf =
      "com.google.protobuf" % "protobuf-java" % protobufVersion
    private val grpcCore        = "io.grpc" % "grpc-core"         % grpcVersion
    private val grpcNettyShaded = "io.grpc" % "grpc-netty-shaded" % grpcVersion
    private val akkaGrpc =
      "com.lightbend.akka.grpc" %% "akka-grpc-runtime" % "1.1.1"
    val all = Seq(protobuf, grpcCore, grpcNettyShaded, akkaGrpc)
  }

  object Akka {
    private val akkaVersion          = "2.6.13"
    private val akkaHttpVersion      = "10.2.4"
    private val alpakkaVersion       = "2.0.2"
    private val akkaHttpCirceVersion = "1.35.3"

    private val sl4j = "com.typesafe.akka" %% "akka-slf4j" % akkaVersion
    private val akkaActorTyped =
      "com.typesafe.akka" %% "akka-actor-typed" % akkaVersion
    private val stream = "com.typesafe.akka" %% "akka-stream" % akkaVersion
    private val circe =
      "de.heikoseeberger" %% "akka-http-circe" % akkaHttpCirceVersion
    private val httpTestKit =
      "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpVersion % Test
    private val streamTestKit =
      "com.typesafe.akka" %% "akka-stream-testkit" % akkaVersion % Test
    private val akkaActorTypedTestKit =
      "com.typesafe.akka" %% "akka-actor-testkit-typed" % akkaVersion % Test
    private val akkaDiscovery =
      "com.typesafe.akka" %% "akka-discovery" % akkaVersion

    val http2 =
      "com.typesafe.akka" %% "akka-http2-support" % akkaHttpVersion
    val http = "com.typesafe.akka" %% "akka-http" % akkaHttpVersion
    val unixDomain =
      "com.lightbend.akka" %% "akka-stream-alpakka-unix-domain-socket" % alpakkaVersion
    val akka = Seq(sl4j,
                   akkaActorTyped,
                   stream,
                   streamTestKit,
                   akkaActorTypedTestKit,
                   akkaDiscovery)
    val akkaHttp = Seq(http, http2, circe, httpTestKit)

    val all = akka ++ akkaHttp
  }

  object Testing {
    private val scalaTest = "org.scalatest" %% "scalatest" % "3.2.6" % Test
    private val scalamock = "org.scalamock" %% "scalamock" % "5.1.0" % Test
    val all               = Seq(scalaTest, scalamock)
  }

  object Cats {
    private val version = "2.4.2"
    private val kernel  = "org.typelevel" %% "cats-kernel" % version
    private val core    = "org.typelevel" %% "cats-core"   % version
    val all             = Seq(kernel, core)
  }

  private lazy val common = Testing.all ++ Logging.all ++ Config.all

  lazy val unisonUi =
    libraryDependencies ++= common ++ Akka.all ++ Circe.all ++ Cats.all

  lazy val unisonUiCore =
    libraryDependencies ++= common ++ Akka.akka ++ Circe.all ++ Grpc.all ++ Seq(
      Akka.http  % Test,
      Akka.http2 % Test,
      Circe.schema,
      Circe.parser)

  lazy val providerContainer =
    libraryDependencies ++= common ++ Akka.all ++ Circe.all ++ Seq(
      Akka.unixDomain,
      "io.skuber" %% "skuber" % "2.6.0")

  lazy val providerGit = libraryDependencies ++= common ++ Akka.all ++ Circe.all

  lazy val providerWebhook =
    libraryDependencies ++= common ++ Akka.all ++ Circe.all
}
