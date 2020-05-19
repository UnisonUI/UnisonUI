import com.typesafe.sbt.packager.docker.DockerPlugin.autoImport.{Docker => docker}
import sbt.Keys._
import sbt.{Project, State, ThisBuild}
import sbtassembly.AssemblyPlugin.autoImport._
import sbtrelease.ExtraReleaseCommands
import sbtrelease.ReleasePlugin.autoImport._
import sbtrelease.ReleaseStateTransformations._
import Tasks._
object Release {
  lazy val dockerReleaseSettings = Seq(
    releaseProcess := Seq(
      checkSnapshotDependencies,
      runTest,
      ReleaseStep(releaseStepTask(webpackProdTask)),
      ReleaseStep(releaseStepTask(publishLocal in docker))
    )
  )
}
