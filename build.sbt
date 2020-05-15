import Aliases._

lazy val root = Project("restUI", file("."))
  .aggregate(projects: _*)
  .settings(crossScalaVersions := Nil)
  .settings(aliases)

lazy val core = (project in file("core"))
  .aggregate(restUiCore, restUi)

lazy val restUi = Projects.restUi
  .dependsOn(restUiCore, serviceDiscoveryDocker)
  .settings(Dependencies.restUi)
  .settings(mainClass in Compile := Some("restui.server.Main"))
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
