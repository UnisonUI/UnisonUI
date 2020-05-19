import Aliases._

scalafixDependencies in ThisBuild += "com.github.liancheng" %% "organize-imports" % "0.3.1-RC1"
testOptions in Test += Tests.Argument(TestFrameworks.ScalaTest, "-u", (baseDirectory.value / "target" / "test-reports").toString, "-o")

lazy val root = Project("restUI", file("."))
  .aggregate(projects: _*)
  .settings(crossScalaVersions := Nil)
  .settings(aliases)
  .disablePlugins(ReleasePlugin)

lazy val restUi = Projects.restUi
  .dependsOn(restUiCore, serviceDiscoveryDocker, serviceDiscoveryKubernetes)
  .settings(Dependencies.restUi)
  .settings(DockerSettings.settings)
  .settings(mainClass in Compile := Some("restui.server.Main"))
  .settings(Tasks.tasks)
  .settings(Release.dockerReleaseSettings)
  .enablePlugins(JavaAppPackaging, DockerPlugin)

lazy val restUiCore = Projects.restUiCore
  .settings(Dependencies.restUiCore)

lazy val serviceDiscovery = (project in file("service-discovery"))
  .aggregate(serviceDiscoveryDocker, serviceDiscoveryKubernetes)
  .disablePlugins(ReleasePlugin)

lazy val serviceDiscoveryKubernetes = Projects.serviceDiscoveryKubernetes
  .dependsOn(restUiCore)
  .settings(Dependencies.serviceDiscoveryKubernetes)
  .disablePlugins(ReleasePlugin)

lazy val serviceDiscoveryDocker = Projects.serviceDiscoveryDocker
  .dependsOn(restUiCore)
  .settings(Dependencies.serviceDiscoveryDocker)
  .disablePlugins(ReleasePlugin)

val projects: Seq[ProjectReference] = Seq(
  restUiCore,
  restUi,
  serviceDiscovery
)
