import com.typesafe.sbt.packager.Keys._
import com.typesafe.sbt.packager.docker._
import sbt._
import sbt.Keys._

object DockerSettings {
  private val confdVersion = "0.16.0"
  private val confdUrl     = s"https://github.com/kelseyhightower/confd/releases/download/v$confdVersion/confd-$confdVersion-linux-amd64"
  val settings = Seq(
    dockerBaseImage := "openjdk:11-jre-slim",
    dockerLabels := Map("maintener" -> "pedro.mangabeiralindekrantz@gmail.com"),
    dockerRepository := Some("docker.pkg.github.com"),
    dockerUsername := Some("maethornaur"),
    dockerUpdateLatest := true,
    dockerExposedPorts := Seq(8080),
    dockerEnvVars := Map("CONFD_BACKEND" -> "env", "CONFD_PREFIX" -> "/"),
    dockerCommands ++= Seq(
      Cmd("USER", "root"),
      Cmd("RUN", "apt-get update -y"),
      Cmd("RUN", "apt-get install -y curl"),
      Cmd("RUN",
          s"""curl -s -L $confdUrl > /usr/local/bin/confd && \\
        | chmod +x /usr/local/bin/confd""".stripMargin),
      Cmd("USER", "1001:0")
    ),
    dockerEntrypoint := Seq("/opt/docker/entrypoint.sh", executableScriptName.value)
  )
}
