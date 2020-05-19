import org.scalafmt.sbt.ScalafmtPlugin.autoImport._
import sbt.Keys._
import sbt._
import scalafix.sbt.ScalafixPlugin.autoImport._
import sbtassembly.AssemblyPlugin.autoImport._
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
    test in assembly := {},
    scalacOptions in Compile ++= ScalacOptions.options,
    updateOptions := updateOptions.value.withGigahorse(false)
  )
}
