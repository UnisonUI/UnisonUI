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
    private val core    = "io.circe" %% "circe-core"        % version
    private val generic = "io.circe" %% "circe-generic"     % version
    private val yaml    = "io.circe" %% "circe-yaml"        % "0.13.1"
    val parser          = "io.circe" %% "circe-parser"      % version
    val schema          = "io.circe" %% "circe-json-schema" % "0.1.0"
    val all             = Seq(core, generic, yaml)
  }

  object Akka {
    private val akkaVersion     = "2.6.8"
    private val akkaHttpVersion = "10.2.0"
    private val alpakka         = "2.0.1"
    private val sl4j            = "com.typesafe.akka"  %% "akka-slf4j"                             % akkaVersion
    private val http            = "com.typesafe.akka"  %% "akka-http"                              % akkaHttpVersion
    private val stream          = "com.typesafe.akka"  %% "akka-stream"                            % akkaVersion
    private val circe           = "de.heikoseeberger"  %% "akka-http-circe"                        % "1.34.0"
    private val httpTestKit     = "com.typesafe.akka"  %% "akka-http-testkit"                      % akkaHttpVersion % Test
    private val streamTestKit   = "com.typesafe.akka"  %% "akka-stream-testkit"                    % akkaVersion     % Test
    val unixDomain              = "com.lightbend.akka" %% "akka-stream-alpakka-unix-domain-socket" % alpakka
    val akka                    = Seq(sl4j, stream, streamTestKit)
    val akkaHttp                = Seq(http, circe, httpTestKit)
    val all                     = akka ++ akkaHttp
  }

  object Testing {
    private val scalaTest = "org.scalatest" %% "scalatest" % "3.2.1" % Test
    private val scalamock = "org.scalamock" %% "scalamock" % "5.0.0" % Test
    val all               = Seq(scalaTest, scalamock)
  }

  private lazy val common = Testing.all ++ Logging.all ++ Config.all

  lazy val restUi = libraryDependencies ++= common ++ Akka.all ++ Circe.all

  lazy val restUiCore = libraryDependencies ++= common ++ Akka.akka ++ Circe.all ++ Seq(Circe.schema, Circe.parser)

  lazy val providerDocker = libraryDependencies ++= common ++ Akka.all ++ Circe.all ++ Seq(Akka.unixDomain)

  lazy val providerKubernetes = libraryDependencies ++= common ++ Akka.all ++
    Seq("io.skuber" %% "skuber" % "2.5.0")

  lazy val providerGit     = libraryDependencies ++= common ++ Akka.all ++ Circe.all
  lazy val providerWebhook = libraryDependencies ++= common ++ Akka.all ++ Circe.all
}
