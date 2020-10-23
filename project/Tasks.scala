import sbt.Keys._
import sbt._
object Tasks {
  lazy val tasks = Seq[Setting[_]](
    npmInstall := {
      import sys.process._
      Process(Seq("npm", "install"), baseDirectory.value / ".." / "web").!
    },
    webpackDevTask := {
      import sys.process._
      Process(Seq("npm", "run", "build:dev"),
              baseDirectory.value / ".." / "web").!
    },
    webpackProdTask := {
      import sys.process._
      Process(Seq("npm", "run", "build:prod"),
              baseDirectory.value / ".." / "web").!
    }
  )
  val npmInstall      = taskKey[Unit]("npm install")
  val webpackDevTask  = taskKey[Unit]("webpack target development")
  val webpackProdTask = taskKey[Unit]("webpack target production")
}
