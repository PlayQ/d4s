package d4s.codecs

import io.circe.{Decoder, Encoder}

package object circe {
  @deprecated("use D4SCirceAttributeEncoder.derived[${T}]")
  def deriveAttribute[T: Encoder]: D4SAttributeEncoder[T] = D4SCirceAttributeEncoder.derived[T]
  @deprecated("use D4SCirceEncoder.derived[${T}]")
  def deriveEncoder[T: Encoder.AsObject]: D4SEncoder[T] = D4SCirceEncoder.derived[T]
  @deprecated("use D4SCirceDecoder.derived[${T}]")
  def deriveDecoder[T: Decoder]: D4SDecoder[T] = D4SCirceDecoder.derived[T]
  @deprecated("use D4SCirceCodec.derived[${T}]")
  def deriveCodec[T: Encoder.AsObject: Decoder]: D4SCodec[T] = D4SCirceCodec.derived[T]
}
