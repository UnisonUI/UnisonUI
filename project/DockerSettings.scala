import com.typesafe.sbt.packager.Keys._
import sbtdocker.DockerPlugin.autoImport._
import sbt._
import sbt.Keys._

object DockerSettings {
  val settings = Seq(
    dockerfile in docker := {
      val confdVersion = "0.16.0"
      val confdUrl =
        s"https://github.com/kelseyhightower/confd/releases/download/v$confdVersion/confd-$confdVersion-linux-amd64"
      val appDir: File = stage.value
      val entrypointFile: File =
        new File(baseDirectory.value, "../docker/entrypoint.sh")
      val targetDir = "/app"
      new Dockerfile {
        from("alpine:3.12.0")
        run("apk",
            "add",
            "--no-cache",
            "openjdk11-jre",
            "git",
            "protoc",
            "bash",
            "curl")
        runShell("curl", "-L", confdUrl, ">", "/usr/local/bin/confd")
        run("chmod", "+x", "/usr/local/bin/confd")
        entryPoint(s"$targetDir/entrypoint.sh", executableScriptName.value)
        workDir(targetDir)
        copy(appDir, targetDir)
      }
    },
    imageNames in docker := Seq(
      ImageName(s"${organization.value}/${name.value}:latest"),
      ImageName(s"${organization.value}/${name.value}:v${version.value}")
    )
  )
}
