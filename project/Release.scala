import com.typesafe.sbt.packager.docker.DockerPlugin.autoImport.{
  Docker => docker
}
import sbt.Keys._
import sbt.{Project, State, ThisBuild}
import sbtassembly.AssemblyPlugin.autoImport._
import sbtrelease.ExtraReleaseCommands
import sbtrelease.ReleasePlugin.autoImport._
import sbtrelease.ReleaseStateTransformations._
import Tasks._
object Release {
  lazy val dockerReleaseSettings = Seq(
    releaseTagComment := s"Releasing ${(version in ThisBuild).value}",
    releaseCommitMessage := s"[skip ci] Setting version to ${(version in ThisBuild).value}",
    releaseNextCommitMessage := s"[skip ci] Setting version to ${(version in ThisBuild).value}",
    releaseProcess := Seq(
      checkSnapshotDependencies,
      inquireVersions,
      setReleaseVersion,
      ReleaseStep(releaseStepTask(npmInstall)),
      ReleaseStep(releaseStepTask(webpackProdTask)),
      ReleaseStep(releaseStepTask(publish in docker)),
      tagRelease,
      setNextVersion,
      commitNextVersion
    )
  )
}
