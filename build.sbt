import Aliases._

scalafixDependencies in ThisBuild += "com.github.liancheng" %% "organize-imports" % "0.3.1-RC1"

val webpackTask = TaskKey[Unit]("webpack", "webpack")
val webpackSettings = webpackTask := {
  import sys.process._
  Process(Seq("npm", "run", "build"), baseDirectory.value).!
}

lazy val root = Project("restUI", file("."))
  .aggregate(projects: _*)
  .settings(crossScalaVersions := Nil)
  .settings(aliases)

lazy val core = (project in file("core"))
  .aggregate(restUiCore, restUi)

lazy val restUi = Projects.restUi
  .dependsOn(restUiCore, serviceDiscoveryDocker)
  .settings(Dependencies.restUi)
  .settings(dockerBaseImage := "openjdk:11-jre-slim")
  .settings(mainClass in Compile := Some("restui.server.Main"))
  .settings(webpackSettings, compile in Compile := (compile in Compile).dependsOn(webpackTask).value)
  .enablePlugins(JavaAppPackaging, DockerPlugin)

lazy val restUiCore = Projects.restUiCore
  .settings(Dependencies.restUiCore)

lazy val serviceDiscovery = (project in file("service-discovery"))
  .aggregate(serviceDiscoveryDocker)

lazy val serviceDiscoveryDocker = Projects.serviceDiscoveryDocker
  .dependsOn(restUiCore)
  .settings(Dependencies.serviceDiscoveryDocker)

val projects: Seq[ProjectReference] = Seq(
  core,
  serviceDiscovery
)
