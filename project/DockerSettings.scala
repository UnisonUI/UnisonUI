import com.typesafe.sbt.packager.Keys._
import com.typesafe.sbt.packager.docker._
import sbt._
import sbt.Keys._

object DockerSettings {
  val settings = Seq(
    dockerBaseImage := "openjdk:11-jre-slim",
    dockerLabels := Map("maintener" -> "pedro.mangabeiralindekrantz@gmail.com"),
    dockerRepository := Some("docker.pkg.github.com"),
    dockerUsername := Some("maethornaur/restui"),
    dockerUpdateLatest := true,
    dockerExposedPorts := Seq(8080),
    dockerEntrypoint := Seq("/opt/docker/entrypoint.sh", executableScriptName.value)
  )
}
