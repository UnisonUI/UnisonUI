import BaseSettings.defaultSettings
import sbt._
import sbtassembly.AssemblyPlugin
import sbtsonar.SonarPlugin.autoImport.sonarProperties

object Projects {

  private def createModule(moduleName: String, fileName: String): Project =
    Project(id = moduleName, base = file(fileName))
      .settings(defaultSettings: _*)
      .settings(sonarProperties ++= SonarProperties.sonarProp(moduleName))
      .enablePlugins(AssemblyPlugin)

  val restUiCore = createModule("rest-ui-core", "core/core")

  val restUi = createModule("rest-ui", "core/rest-ui")

  val serviceDiscoveryDocker = createModule("service-discovery-docker", "service-discovery/docker")
}
