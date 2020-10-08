import com.typesafe.sbt.packager.Keys._
import com.typesafe.sbt.packager.docker._
import com.typesafe.sbt.packager.docker.DockerPlugin.autoImport.Docker
import sbt._
import sbt.Keys._

object DockerSettings {
  val settings = Seq(
    dockerBaseImage := "openjdk:11-jre-slim",
    dockerLabels := Map("maintener" -> "pedro.mangabeiralindekrantz@gmail.com"),
    dockerUsername := Some("maethornaur"),
    dockerRepository := Some("ghcr.io"),
    dockerUpdateLatest := true,
    packageName in Docker := "restui",
    dockerExposedPorts := Seq(8080),
    dockerEntrypoint := Seq("/opt/docker/entrypoint.sh", executableScriptName.value),
    dockerCommands ++= Seq(
      Cmd("USER", "root"),
      Cmd("RUN", "apt-get update -y && apt-get install -y git protobuf-compiler"),
      Cmd("USER", "1001:0")
    )
  )
}
