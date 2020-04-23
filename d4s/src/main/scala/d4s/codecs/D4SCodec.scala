package d4s.codecs

import java.util

import d4s.codecs.D4SCodec.fromPair
import d4s.models.DynamoException.DynamoDecoderException
import software.amazon.awssdk.services.dynamodb.model.AttributeValue

trait D4SCodec[A] extends D4SAttributeCodec[A] with D4SEncoder[A] {
  final def imap2[B, C](another: D4SCodec[B])(to: (A, B) => C)(from: C => (A, B)): D4SCodec[C] = {
    fromPair(contramap2(another)(from), map2(another)(to))
  }
  final def imapObject(
    to: Map[String, AttributeValue] => Map[String, AttributeValue]
  )(from: Map[String, AttributeValue] => Map[String, AttributeValue]): D4SCodec[A] = {
    fromPair(mapObject(to), contramapObject(from))
  }
}

object D4SCodec {
  def apply[A: D4SCodec]: D4SCodec[A]                                    = implicitly
  def derived[A](implicit derivedCodec: D4SDerivedCodec[A]): D4SCodec[A] = D4SCodec.fromPair(derivedCodec.enc, derivedCodec.dec)

  def from[T: D4SEncoder: D4SDecoder]: D4SCodec[T] = fromPair(D4SEncoder[T], D4SDecoder[T])
  def fromPair[T](encoder: D4SEncoder[T], decoder: D4SDecoder[T]): D4SCodec[T] = new D4SCodec[T] {
    override def encode(item: T): Map[String, AttributeValue]          = encoder.encode(item)
    override def encodeJava(item: T): util.Map[String, AttributeValue] = encoder.encodeJava(item)

    override def decode(item: Map[String, AttributeValue]): Either[DynamoDecoderException, T]      = decoder.decode(item)
    override def decode(item: util.Map[String, AttributeValue]): Either[DynamoDecoderException, T] = decoder.decode(item)
    override def decodeAttribute(attr: AttributeValue): Either[DynamoDecoderException, T]          = decoder.decodeAttribute(attr)
  }

  @deprecated("Use derived", "1.0.3")
  def derive[A](implicit derivedCodec: D4SDerivedCodec[A]): D4SCodec[A] = D4SCodec.fromPair(derivedCodec.enc, derivedCodec.dec)
}
