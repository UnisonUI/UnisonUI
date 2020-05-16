import sbt.Keys._
import sbt._

object Dependencies {

  object Config {
    private val config = "com.typesafe" % "config" % "1.4.0"
    val all            = Seq(config)

  }
  object Logging {
    private val logback = "ch.qos.logback" % "logback-classic" % "1.2.3"
    val all             = Seq(logback)

  }
  object Circe {
    private val version = "0.12.3"
    private val core    = "io.circe" %% "circe-core"    % version
    private val generic = "io.circe" %% "circe-generic" % version
    val all             = Seq(core, generic)
  }

  object Akka {
    private val akkaVersion     = "2.6.5"
    private val akkaHttpVersion = "10.1.12"
    private val sl4j            = "com.typesafe.akka" %% "akka-slf4j"          % akkaVersion
    private val http            = "com.typesafe.akka" %% "akka-http"           % akkaHttpVersion
    private val cors            = "ch.megard"         %% "akka-http-cors"      % "0.4.3"
    private val circe           = "de.heikoseeberger" %% "akka-http-circe"     % "1.31.0"
    private val httpTestKit     = "com.typesafe.akka" %% "akka-http-testkit"   % akkaHttpVersion % Test
    private val streamTestKit   = "com.typesafe.akka" %% "akka-stream-testkit" % akkaVersion     % Test
    val all                     = Seq(sl4j, http, cors, circe, httpTestKit, streamTestKit)
  }

  object Testing {
    private val scalaTest = "org.scalatest" %% "scalatest" % "3.1.2" % Test
    val all               = Seq(scalaTest)
  }

  private lazy val common = Testing.all ++ Logging.all ++ Config.all

  lazy val restUi = libraryDependencies ++= common ++ Akka.all ++ Circe.all

  lazy val restUiCore = libraryDependencies ++= common

  lazy val serviceDiscoveryDocker = libraryDependencies ++= common ++
    Seq(
      "com.github.docker-java" % "docker-java" % "3.2.1" exclude
        ("com.github.docker-java", "docker-java-transport-jersey"))
}
