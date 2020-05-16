import BaseSettings.defaultSettings
import sbt._
import sbtassembly.AssemblyPlugin
import sbtassembly.AssemblyPlugin.autoImport._
import sbtsonar.SonarPlugin.autoImport.sonarProperties

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

  val restUiCore = createModule("rest-ui-core", "core/core")

  val restUi = createModule("rest-ui", "core/rest-ui")

  val serviceDiscoveryDocker = createModule("service-discovery-docker", "service-discovery/docker")
}
