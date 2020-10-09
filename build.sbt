import Aliases._

scalafixDependencies in ThisBuild += "com.github.liancheng" %% "organize-imports" % "0.3.1-RC1"

testOptions in Test += Tests.Argument(
  TestFrameworks.ScalaTest,
  "-u",
  (baseDirectory.value / "target" / "test-reports").toString,
  "-o")

lazy val root = Project("restUI", file("."))
  .aggregate(projects: _*)
  .settings(crossScalaVersions := Nil)
  .settings(aliases)
  .disablePlugins(ReleasePlugin)

lazy val restUi = Projects.restUi
  .dependsOn(restUiCore,
             providerDocker,
             providerKubernetes,
             providerGit,
             providerWebhook)
  .settings(Dependencies.restUi)
  .settings(DockerSettings.settings)
  .settings(mainClass in Compile := Some("restui.server.Main"))
  .settings(Tasks.tasks)
  .settings(Release.dockerReleaseSettings)
  .enablePlugins(JavaAppPackaging, DockerPlugin)

lazy val restUiCore = Projects.restUiCore
  .settings(Dependencies.restUiCore)

lazy val providers = (project in file("providers"))
  .aggregate(providerDocker, providerKubernetes, providerGit, providerWebhook)
  .disablePlugins(ReleasePlugin)

lazy val providerWebhook = Projects.providerWebhook
  .dependsOn(restUiCore)
  .settings(Dependencies.providerWebhook)
  .disablePlugins(ReleasePlugin)

lazy val providerGit = Projects.providerGit
  .dependsOn(restUiCore)
  .settings(Dependencies.providerGit)
  .disablePlugins(ReleasePlugin)

lazy val providerKubernetes = Projects.providerKubernetes
  .dependsOn(restUiCore)
  .settings(Dependencies.providerKubernetes)
  .disablePlugins(ReleasePlugin)

lazy val providerDocker = Projects.providerDocker
  .dependsOn(restUiCore)
  .settings(Dependencies.providerDocker)
  .disablePlugins(ReleasePlugin)

val projects: Seq[ProjectReference] = Seq(
  restUiCore,
  restUi,
  providers
)
