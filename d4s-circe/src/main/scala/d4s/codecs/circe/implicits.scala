package d4s.codecs.circe

import d4s.codecs.{D4SAttributeEncoder, D4SDecoder, D4SEncoder}
import io.circe.{Decoder, Encoder}

import scala.language.implicitConversions

object implicits {
  implicit def fromEncoder[T](enc: Encoder[T]): D4SAttributeEncoder[T] = deriveAttribute[T](enc)
  implicit def fromEncoder[T](enc: Encoder.AsObject[T]): D4SEncoder[T] = deriveEncoder(enc)
  implicit def fromDecoder[T](dec: Decoder[T]): D4SDecoder[T]          = deriveDecoder[T](dec)
}
