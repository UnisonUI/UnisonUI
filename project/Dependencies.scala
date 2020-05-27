import sbt.Keys._
import sbt._

object Dependencies {

  object Config {
    private val config = "com.typesafe" % "config" % "1.4.0"
    val all            = Seq(config)

  }
  object Logging {
    private val logback      = "ch.qos.logback"              % "logback-classic" % "1.2.3"
    private val scalaLogging = "com.typesafe.scala-logging" %% "scala-logging"   % "3.9.2"
    val all                  = Seq(logback, scalaLogging)

  }
  object Circe {
    private val version = "0.13.0"
    private val core    = "io.circe" %% "circe-core"    % version
    private val generic = "io.circe" %% "circe-generic" % version
    private val yaml    = "io.circe" %% "circe-yaml"    % version
    val all             = Seq(core, generic, yaml)
  }

  object Akka {
    private val akkaVersion     = "2.6.5"
    private val akkaHttpVersion = "10.1.12"
    private val sl4j            = "com.typesafe.akka" %% "akka-slf4j"          % akkaVersion
    private val http            = "com.typesafe.akka" %% "akka-http"           % akkaHttpVersion
    private val stream          = "com.typesafe.akka" %% "akka-stream"         % akkaVersion
    private val circe           = "de.heikoseeberger" %% "akka-http-circe"     % "1.32.0"
    private val httpTestKit     = "com.typesafe.akka" %% "akka-http-testkit"   % akkaHttpVersion % Test
    private val streamTestKit   = "com.typesafe.akka" %% "akka-stream-testkit" % akkaVersion     % Test
    val akka                    = Seq(sl4j, stream, streamTestKit)
    val akkaHttp                = Seq(http, circe, httpTestKit)
    val all                     = akka ++ akkaHttp
  }

  object Testing {
    private val scalaTest = "org.scalatest" %% "scalatest" % "3.1.2" % Test
    private val scalamock = "org.scalamock" %% "scalamock" % "4.4.0" % Test
    val all               = Seq(scalaTest, scalamock)
  }

  private lazy val common = Testing.all ++ Logging.all ++ Config.all

  lazy val restUi = libraryDependencies ++= common ++ Akka.all ++ Circe.all

  lazy val restUiCore = libraryDependencies ++= common ++ Akka.akka

  lazy val providerDocker = libraryDependencies ++= common ++ Akka.all ++
    Seq(
      "com.github.docker-java" % "docker-java" % "3.2.1" exclude
        ("com.github.docker-java", "docker-java-transport-jersey"))

  lazy val providerKubernetes = libraryDependencies ++= common ++ Akka.all ++
    Seq("io.skuber" %% "skuber" % "2.4.0")

  lazy val providerGit = libraryDependencies ++= common ++ Akka.all ++ Circe.all
}
