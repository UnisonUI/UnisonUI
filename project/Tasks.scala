import sbt.Keys._
import sbt._
object Tasks {
  lazy val tasks = Seq[Setting[_]](
    npmInstall := {
      import sys.process._
      Process(Seq("npm", "install"), baseDirectory.value).!
    },
    webpackDevTask := {
      import sys.process._
      Process(Seq("npm", "run", "build:dev"), baseDirectory.value).!
    },
    webpackProdTask := {
      import sys.process._
      Process(Seq("npm", "run", "build:prod"), baseDirectory.value).!
    }
  )
  val npmInstall      = taskKey[Unit]("npm install")
  val webpackDevTask  = taskKey[Unit]("webpack target development")
  val webpackProdTask = taskKey[Unit]("webpack target production")
}
