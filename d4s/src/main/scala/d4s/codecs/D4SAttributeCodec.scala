package d4s.codecs

import java.util

import d4s.codecs.CodecsUtils.DynamoDecoderException
import d4s.codecs.D4SAttributeCodec.fromPair
import software.amazon.awssdk.services.dynamodb.model.AttributeValue

trait D4SAttributeCodec[A] extends D4SAttributeEncoder[A] with D4SDecoder[A] {
  final def imap[B](to: A => B)(from: B => A): D4SAttributeCodec[B] = fromPair(contramap(from), map(to))
}

object D4SAttributeCodec {
  def apply[A: D4SAttributeCodec]: D4SAttributeCodec[A]                                    = implicitly
  def derived[A](implicit derivedCodec: D4SDerivedAttributeCodec[A]): D4SAttributeCodec[A] = D4SAttributeCodec.fromPair(derivedCodec.enc, derivedCodec.dec)

  def from[T: D4SAttributeEncoder: D4SDecoder]: D4SAttributeCodec[T] = fromPair(D4SAttributeEncoder[T], D4SDecoder[T])
  def fromPair[T](encoder: D4SAttributeEncoder[T], decoder: D4SDecoder[T]): D4SAttributeCodec[T] = new D4SAttributeCodec[T] {
    override def encodeAttribute(item: T): AttributeValue = encoder.encodeAttribute(item)

    override def decode(item: Map[String, AttributeValue]): Either[DynamoDecoderException, T]      = decoder.decode(item)
    override def decode(item: util.Map[String, AttributeValue]): Either[DynamoDecoderException, T] = decoder.decode(item)
    override def decodeAttribute(attr: AttributeValue): Either[DynamoDecoderException, T]          = decoder.decodeAttribute(attr)
  }
}
