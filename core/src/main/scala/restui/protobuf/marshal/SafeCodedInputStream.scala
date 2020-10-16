package restui.protobuf.marshal

import java.io.InputStream
import java.nio.ByteBuffer

import com.google.protobuf.{CodedInputStream, WireFormat}

import scala.util.chaining._
import scala.util.control.Exception.allCatch

object SafeCodedInputStream {
  def apply(inputStream: InputStream): SafeCodedInputStream =
    inputStream
      .pipe(CodedInputStream.newInstance)
      .pipe(new SafeCodedInputStream(_))
  def apply(byteBuffer: ByteBuffer): SafeCodedInputStream =
    byteBuffer
      .pipe(CodedInputStream.newInstance)
      .pipe(new SafeCodedInputStream(_))
  def apply(bytes: Array[Byte]): SafeCodedInputStream =
    bytes.pipe(CodedInputStream.newInstance).pipe(new SafeCodedInputStream(_))
}

class SafeCodedInputStream(private val in: CodedInputStream) {
// $COVERAGE-OFF$
  def readFieldId: Either[Throwable, Int] =
    allCatch.either(in.readTag().pipe(WireFormat.getTagFieldNumber))
  def isAtEnd: Either[Throwable, Boolean]   = allCatch.either(in.isAtEnd())
  def readFloat: Either[Throwable, Float]   = allCatch.either(in.readFloat())
  def readDouble: Either[Throwable, Double] = allCatch.either(in.readDouble())
  def readFixed32: Either[Throwable, Int]   = allCatch.either(in.readFixed32())
  def readInt32: Either[Throwable, Int]     = allCatch.either(in.readInt32())
  def readUInt32: Either[Throwable, Int]    = allCatch.either(in.readUInt32())
  def readSFixed32: Either[Throwable, Int]  = allCatch.either(in.readSFixed32())
  def readSInt32: Either[Throwable, Int]    = allCatch.either(in.readSInt32())
  def readFixed64: Either[Throwable, Long]  = allCatch.either(in.readFixed64())
  def readInt64: Either[Throwable, Long]    = allCatch.either(in.readInt64())
  def readUInt64: Either[Throwable, Long]   = allCatch.either(in.readUInt64())
  def readSFixed64: Either[Throwable, Long] = allCatch.either(in.readSFixed64())
  def readSInt64: Either[Throwable, Long]   = allCatch.either(in.readSInt64())
  def readBool: Either[Throwable, Boolean]  = allCatch.either(in.readBool())
  def readString: Either[Throwable, String] = allCatch.either(in.readString())
  def readByteArray: Either[Throwable, Array[Byte]] =
    allCatch.either(in.readByteArray())
  def readByteBuffer: Either[Throwable, ByteBuffer] =
    allCatch.either(in.readByteBuffer())
  def readEnum: Either[Throwable, Int] = allCatch.either(in.readEnum())
// $COVERAGE-ON$
}
