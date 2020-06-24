package d4s.codecs

import d4s.codecs.D4SAttributeCodec.fromPair
import d4s.codecs.WithD4S.D4SDerivedAttributeCodec
import d4s.models.DynamoException.DecoderException
import software.amazon.awssdk.services.dynamodb.model.AttributeValue

trait D4SAttributeCodec[A] extends D4SAttributeEncoder[A] with D4SDecoder[A] {
  def imap[B](decode: A => B)(encode: B => A): D4SAttributeCodec[B] = fromPair(contramap(encode), map(decode))
}

object D4SAttributeCodec {
  @inline def apply[A](implicit ev: D4SAttributeCodec[A]): ev.type = ev

  def derived[A](implicit derivedCodec: D4SDerivedAttributeCodec[A]): D4SAttributeCodec[A] = D4SAttributeCodec.fromPair(derivedCodec.enc, derivedCodec.dec)

  def from[T: D4SAttributeEncoder: D4SDecoder]: D4SAttributeCodec[T] = fromPair(D4SAttributeEncoder[T], D4SDecoder[T])
  def fromPair[T](encoder: D4SAttributeEncoder[T], decoder: D4SDecoder[T]): D4SAttributeCodec[T] = new D4SAttributeCodec[T] {
    override def encode(item: T): AttributeValue = encoder.encode(item)

    override def decodeObject(item: Map[String, AttributeValue]): Either[DecoderException, T] = decoder.decodeObject(item)
    override def decode(attr: AttributeValue): Either[DecoderException, T]                    = decoder.decode(attr)
  }
}
