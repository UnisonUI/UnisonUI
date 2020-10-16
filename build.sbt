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
  .dependsOn(unisonUiCore,
             providerDocker,
             providerKubernetes,
             providerGit,
             providerWebhook)
  .settings(Dependencies.unisonUi)
  .settings(DockerSettings.settings)
  .settings(mainClass in Compile := Some("tech.unisonui.server.Main"))
  .settings(Tasks.tasks)
  .enablePlugins(JavaAppPackaging, sbtdocker.DockerPlugin)

lazy val unisonUiCore = Projects.unisonUiCore
  .settings(Dependencies.unisonUiCore)

lazy val providers = (project in file("providers"))
  .aggregate(providerDocker, providerKubernetes, providerGit, providerWebhook)

lazy val providerWebhook = Projects.providerWebhook
  .dependsOn(unisonUiCore)
  .settings(Dependencies.providerWebhook)

lazy val providerGit = Projects.providerGit
  .dependsOn(unisonUiCore)
  .settings(Dependencies.providerGit)

lazy val providerKubernetes = Projects.providerKubernetes
  .dependsOn(unisonUiCore)
  .settings(Dependencies.providerKubernetes)

lazy val providerDocker = Projects.providerDocker
  .dependsOn(unisonUiCore)
  .settings(Dependencies.providerDocker)

val projects: Seq[ProjectReference] = Seq(
  unisonUiCore,
  unisonUi,
  providers
)
