package tech.unisonui.protobuf

import cats.syntax.functor._
import io.circe.syntax._
import io.circe.{Decoder, Encoder}

package object json {
  implicit val encoder: Encoder[Any] = Encoder.instance {
    case value: String     => value.asJson
    case value: BigDecimal => value.asJson
    case value: BigInt     => value.asJson
    case value: Boolean    => value.asJson
    case value: Byte       => value.asJson
    case value: Double     => value.asJson
    case value: Float      => value.asJson
    case value: Int        => value.asJson
    case value: Long       => value.asJson
    case value: Short      => value.asJson
  }

  implicit val decoder: Decoder[Any] = List[Decoder[Any]](
    Decoder.decodeString.widen,
    Decoder.decodeBigDecimal.widen,
    Decoder.decodeBigInt.widen,
    Decoder.decodeBoolean.widen,
    Decoder.decodeByte.widen,
    Decoder.decodeDouble.widen,
    Decoder.decodeFloat.widen,
    Decoder.decodeInt.widen,
    Decoder.decodeLong.widen,
    Decoder.decodeShort.widen,
    Decoder.const(None)
  ).reduceLeft(_ or _)
}
