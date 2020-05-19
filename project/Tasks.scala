import sbt._
import sbt.Keys._
object Tasks {
  val webpackDevTask  = taskKey[Unit]("webpack dev")
  val webpackProdTask = taskKey[Unit]("webpack prod")
  lazy val tasks = Seq[Setting[_]](
    webpackDevTask := {
      import sys.process._
      Process(Seq("npm", "run", "build"), baseDirectory.value).!
    },
    webpackProdTask := {
      import sys.process._
      Process(Seq("npm", "run", "prod"), baseDirectory.value).!
    }
  )
}
