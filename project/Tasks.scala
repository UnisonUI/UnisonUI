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
      Process(Seq("npm", "run", "build"), baseDirectory.value).!
    },
    webpackProdTask := {
      import sys.process._
      Process(Seq("npm", "run", "prod"), baseDirectory.value).!
    }
  )
  val npmInstall      = taskKey[Unit]("npm install")
  val webpackDevTask  = taskKey[Unit]("webpack dev")
  val webpackProdTask = taskKey[Unit]("webpack prod")
}
