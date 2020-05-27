import com.typesafe.sbt.packager.Keys._
import com.typesafe.sbt.packager.docker._
import sbt._
import sbt.Keys._

object DockerSettings {
  val settings = Seq(
    dockerBaseImage := "openjdk:11-jre-slim",
    dockerLabels := Map("maintener" -> "pedro.mangabeiralindekrantz@gmail.com"),
    dockerUsername := Some("maethornaur"),
    dockerUpdateLatest := true,
    dockerExposedPorts := Seq(8080),
    dockerEntrypoint := Seq("/opt/docker/entrypoint.sh", executableScriptName.value),
    dockerCommands ++= Seq(
      Cmd("USER", "root"),
      Cmd("RUN", "apt-get update -y && apt-get install -y git"),
      Cmd("USER", "1001:0")
    )
  )
}
