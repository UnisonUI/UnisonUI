import sbt._
import sbt.Keys._
import com.typesafe.sbt.packager.universal.UniversalPlugin.autoImport._
import sbtassembly.AssemblyPlugin
import sbtassembly.AssemblyPlugin.autoImport._
import sbtsonar.SonarPlugin.autoImport.sonarProperties

import BaseSettings.defaultSettings

object Projects {

  private def createModule(moduleName: String, fileName: String): Project =
    Project(id = moduleName, base = file(fileName))
      .settings(defaultSettings: _*)
      .settings(sonarProperties ++= SonarProperties.sonarProp(moduleName))
      .enablePlugins(AssemblyPlugin)
      .settings(assemblyMergeStrategy in assembly := {
        case PathList("META-INF", "io.netty.versions.properties", xs @ _*) => MergeStrategy.last
        case "application.conf"                                            => MergeStrategy.concat
        case "reference.conf"                                              => MergeStrategy.concat
        case PathList("META-INF", "services", _*)                          => MergeStrategy.concat
        case PathList("META-INF", _*)                                      => MergeStrategy.discard
        case x                                                             => MergeStrategy.last
      })

  val restUiCore = createModule("rest-ui-core", "core")
    .settings(resolvers += "jitpack".at("https://jitpack.io"))

  val restUi = createModule("rest-ui", "rest-ui")
    .settings(mappings in Universal += file("docker/entrypoint.sh") -> "entrypoint.sh")

  val providerDocker     = createModule("provider-docker", "providers/docker")
  val providerKubernetes = createModule("provider-kubernetes", "providers/kubernetes")
  val providerGit        = createModule("provider-git", "providers/git")
  val providerWebhook    = createModule("provider-webhook", "providers/webhook")
}
