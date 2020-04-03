package d4s.codecs

import io.circe.{Decoder, Encoder}

package object circe {
  def deriveAttribute[T: Encoder]: D4SAttributeEncoder[T]    = D4SCirceAttributeEncoder.derived[T]
  def deriveEncoder[T: Encoder.AsObject]: D4SEncoder[T]      = D4SCirceEncoder.derived[T]
  def deriveDecoder[T: Decoder]: D4SDecoder[T]               = D4SCirceDecoder.derived[T]
  def deriveCodec[T: Encoder.AsObject: Decoder]: D4SCodec[T] = D4SCirceCodec.derive[T]
}
