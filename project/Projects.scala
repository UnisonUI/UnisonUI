import sbt._
import sbt.Keys._
import com.typesafe.sbt.packager.Keys._
import com.typesafe.sbt.packager.universal.UniversalPlugin.autoImport.Universal
import com.typesafe.sbt.SbtNativePackager.autoImport.NativePackagerHelper._
import sbtassembly.AssemblyPlugin
import sbtassembly.AssemblyPlugin.autoImport._
import BaseSettings.defaultSettings

object Projects {

  private def createModule(moduleName: String, fileName: String): Project =
    Project(id = moduleName, base = file(fileName))
      .settings(defaultSettings: _*)
      .enablePlugins(AssemblyPlugin)
      .settings(assemblyMergeStrategy in assembly := {
        case PathList("META-INF", "io.netty.versions.properties", xs @ _*) =>
          MergeStrategy.last
        case "application.conf"                   => MergeStrategy.concat
        case "reference.conf"                     => MergeStrategy.concat
        case PathList("META-INF", "services", _*) => MergeStrategy.concat
        case PathList("META-INF", _*)             => MergeStrategy.discard
        case x                                    => MergeStrategy.last
      })

  val unisonUiCore = createModule("unison-ui-core", "modules/core")
    .settings(resolvers += "jitpack".at("https://jitpack.io"))

  val unisonUi = createModule("unison-ui", "modules/unison-ui")
    .settings(
      name := "unisonui",
      packageName in Universal := name.value,
      mappings in Universal ++= directory("docker/statics"),
      topLevelDirectory in Universal := Some(packageName.value)
    )

  val providerContainer =
    createModule("provider-container", "modules/providers/container")
  val providerGit = createModule("provider-git", "modules/providers/git")
  val providerWebhook =
    createModule("provider-webhook", "modules/providers/webhook")
}
