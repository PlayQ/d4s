package d4s.codecs

import software.amazon.awssdk.services.dynamodb.model.AttributeValue
import scala.language.implicitConversions

trait D4SCodec[A] extends D4SAttributeEncoder[A] with D4SAttributeDecoder[A] {
  self =>

  def encoder: D4SEncoder[A]
  def decoder: D4SDecoder[A]

  final def encodeAttribute(item: A): AttributeValue = encoder.encodeAttribute(item)
  final def decodeAttribute(item: AttributeValue): Either[CodecsUtils.DynamoDecoderException, A] = decoder.decodeAttribute(item)

  final def encode(item: A): Map[String, AttributeValue]                                             = encoder.encode(item)
  final def decode(item: Map[String, AttributeValue]): Either[CodecsUtils.DynamoDecoderException, A] = decoder.decode(item)

  final def timap[B](to: A => B)(from: B => A): D4SCodec[B] = new D4SCodec[B] {
    override val encoder: D4SEncoder[B] = self.encoder.contramap(from)
    override val decoder: D4SDecoder[B] = self.decoder.map(to)
  }
  final def timap2[B, C](another: D4SCodec[B])(to: (A, B) => C)(from: C => (A, B)): D4SCodec[C] = new D4SCodec[C] {
    override val encoder: D4SEncoder[C] = self.encoder.contramap2(another.encoder)(from)
    override val decoder: D4SDecoder[C] = self.decoder.map2(another.decoder)(to)
  }
}

object D4SCodec {
  def apply[A: D4SCodec]: D4SCodec[A] = implicitly

  def derive[A](implicit derivedCodec: DerivationDerivedCodec[A]): D4SCodec[A] = new D4SCodec[A] {
    val encoder: D4SEncoder[A] = derivedCodec.enc
    val decoder: D4SDecoder[A] = derivedCodec.dec
  }
}
