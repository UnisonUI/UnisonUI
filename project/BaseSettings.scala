import org.scalafmt.sbt.ScalafmtPlugin.autoImport._
import sbt.Keys._
import sbt._
import scalafix.sbt.ScalafixPlugin.autoImport._
import Dependencies._
object BaseSettings {

  val ScalaVersion = "2.13.2"

  lazy val defaultSettings = Seq(
    startYear := Some(2020),
    organization := "restui",
    scalaVersion := ScalaVersion,
    parallelExecution in Test := false,
    javaOptions in Test += "-Xmx4G",
    fork in Test := true,
    cancelable in Global := true,
    addCompilerPlugin("org.scalameta" % "semanticdb-scalac" % "4.3.10" cross CrossVersion.full),
    scalacOptions += "-Yrangepos",
    scalacOptions in Compile ++= Seq(
      "-encoding",
      "UTF-8",
      "-deprecation",
      "-unchecked",
      "-Yrangepos",
      "-Ywarn-dead-code",
      "-Ywarn-unused",
      "-Wunused:imports",
      "-language:implicitConversions",
      "-language:higherKinds",
      "-target:jvm-1.8",
      "-feature"
    ),
    run in Compile := Defaults
      .runTask(fullClasspath in Compile, mainClass in (Compile, run), runner in (Compile, run))
      .evaluated, // put provided dependencies back in scope for 'run'
    updateOptions := updateOptions.value.withGigahorse(false)
  )
}
