import Aliases._

scalafixDependencies in ThisBuild += "com.github.liancheng" %% "organize-imports" % "0.4.2"

testOptions in Test += Tests.Argument(
  TestFrameworks.ScalaTest,
  "-u",
  (baseDirectory.value / "target" / "test-reports").toString,
  "-o")

lazy val root = Project("UnisonUI", file("."))
  .aggregate(projects: _*)
  .settings(crossScalaVersions := Nil)
  .settings(aliases)

lazy val unisonUi = Projects.unisonUi
  .dependsOn(unisonUiCore, providerContainer, providerGit, providerWebhook)
  .settings(Dependencies.unisonUi)
  .settings(DockerSettings.settings)
  .settings(mainClass in Compile := Some("tech.unisonui.server.Main"))
  .settings(Tasks.tasks)
  .enablePlugins(JavaAppPackaging, sbtdocker.DockerPlugin)

lazy val unisonUiCore = Projects.unisonUiCore
  .settings(Dependencies.unisonUiCore)

lazy val providers = (project in file("providers"))
  .aggregate(providerContainer, providerGit, providerWebhook)

lazy val providerWebhook = Projects.providerWebhook
  .dependsOn(unisonUiCore)
  .settings(Dependencies.providerWebhook)

lazy val providerGit = Projects.providerGit
  .dependsOn(unisonUiCore)
  .settings(Dependencies.providerGit)

lazy val providerContainer = Projects.providerContainer
  .dependsOn(unisonUiCore)
  .settings(Dependencies.providerContainer)

val projects: Seq[ProjectReference] = Seq(
  unisonUiCore,
  unisonUi,
  providers
)
