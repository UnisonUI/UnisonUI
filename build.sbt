import Aliases._
import BaseSettings._
import Dependencies._

lazy val root = (project in file("."))
  .aggregate(`core`, `service-discovery`)
  .settings(crossScalaVersions := Nil)
  .settings(aliases)

lazy val `core` = (project in file("core"))
  .aggregate(`rest-ui-core`, `rest-ui`)

lazy val `service-discovery` = (project in file("service-discovery"))
  .aggregate(`rest-ui-service-discovery-docker`)

lazy val `rest-ui` = module("rest-ui", file("core/rest-ui"))
  .dependsOn(`rest-ui-core`, `rest-ui-service-discovery-docker`)
  .settings(libraryDependencies ++= Akka.all ++ Circe.all ++ Html.all)
  .settings(mainClass in Compile := Some("restui.server.Main"))
  .enablePlugins(JavaAppPackaging, DockerPlugin)

lazy val `rest-ui-core` = module("rest-ui-core", file("core/core"))

lazy val `rest-ui-service-discovery-docker` = module("service-discovery-docker", file("service-discovery/docker"))
  .dependsOn(`rest-ui-core`)
  .settings(
    libraryDependencies += "com.github.docker-java" % "docker-java" % "3.2.1" exclude ("com.github.docker-java", "docker-java-transport-jersey"))

def module(name: String, base: File): Project =
  Project(id = name, base = base)
    .settings(defaultSettings: _*)
    .settings(libraryDependencies ++= Testing.all ++ Logging.all ++ Config.all)
    .settings(sonarProperties ++= Map(
      "sonar.sources"                    -> s"$name/src/main/scala",
      "sonar.tests"                      -> s"$name/src/test/scala",
      "sonar.junit.reportPaths"          -> s"$name/target/test-reports",
      "sonar.scala.scapegoat.disable"    -> "true",
      "sonar.scala.scoverage.reportPath" -> s"$name/scala-2.13/scoverage-report/scoverage.xml"
    ))
    .enablePlugins(AssemblyPlugin)
