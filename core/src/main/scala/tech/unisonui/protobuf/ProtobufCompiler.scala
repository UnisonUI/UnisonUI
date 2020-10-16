package tech.unisonui.protobuf

import java.io.File
import java.nio.file.Path

import scala.sys.process._
import scala.util.chaining._
import scala.util.control.Exception.allCatch

trait ProtobufCompiler {
  def compile(path: Path): Either[Throwable, File]
  def clean(file: File): Either[Throwable, Unit]
}

class ProtobufCompilerImpl extends ProtobufCompiler {
  private val protocExe = "protoc"
  override def compile(path: Path): Either[Throwable, File] =
    allCatch.either {
      val tempFile = File.createTempFile("unisonui", ".protoset")
      Seq(protocExe,
          s"-o${tempFile.getAbsolutePath}",
          "--include_imports",
          "-I",
          path.toAbsolutePath.getParent.toString,
          path.toAbsolutePath.toString)
        .pipe(Process(_))
        .!!
      tempFile
    }
  override def clean(file: File): Either[Throwable, Unit] =
    allCatch.either(file.delete())
}
