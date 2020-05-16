import Aliases._

scalafixDependencies in ThisBuild += "com.github.liancheng" %% "organize-imports" % "0.3.1-RC1"

lazy val root = Project("restUI", file("."))
  .aggregate(projects: _*)
  .settings(crossScalaVersions := Nil)
  .settings(aliases)
  .disablePlugins(ReleasePlugin)

lazy val core = (project in file("core"))
  .aggregate(restUiCore, restUi)

lazy val restUi = Projects.restUi
  .dependsOn(restUiCore, serviceDiscoveryDocker)
  .settings(Dependencies.restUi)
  .settings(dockerBaseImage := "openjdk:11-jre-slim")
  .settings(mainClass in Compile := Some("restui.server.Main"))
  .settings(Tasks.tasks)
  .settings(Release.dockerReleaseSettings)
  .enablePlugins(JavaAppPackaging, DockerPlugin)

lazy val restUiCore = Projects.restUiCore
  .settings(Dependencies.restUiCore)

lazy val serviceDiscovery = (project in file("service-discovery"))
  .aggregate(serviceDiscoveryDocker)
  .disablePlugins(ReleasePlugin)

lazy val serviceDiscoveryDocker = Projects.serviceDiscoveryDocker
  .dependsOn(restUiCore)
  .settings(Dependencies.serviceDiscoveryDocker)
  .disablePlugins(ReleasePlugin)

val projects: Seq[ProjectReference] = Seq(
  core,
  serviceDiscovery
)
