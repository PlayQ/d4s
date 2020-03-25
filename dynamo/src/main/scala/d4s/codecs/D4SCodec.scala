package d4s.codecs

import software.amazon.awssdk.services.dynamodb.model.AttributeValue

trait D4SCodec[A] extends D4SEncoder[A] with D4SDecoder[A] {
  def encode(item: A): Map[String, AttributeValue]
  def decode(item: Map[String, AttributeValue]): Either[CodecsUtils.DynamoDecoderException, A]

  def imap[B](to: A => B)(from: B => A): D4SCodec[B]
  def imap2[B, C](another: D4SCodec[B])(to: (A, B) => C)(from: C => (A, B)): D4SCodec[C]
}

object D4SCodec {
  def apply[A: D4SCodec]: D4SCodec[A] = implicitly

  def fromPair[T](encoder: D4SEncoder[T], decoder: D4SDecoder[T]): D4SCodec[T] = new D4SCodec[T] {
    override def encode(item: T): Map[String, AttributeValue]                                             = encoder.encode(item)
    override def decode(item: Map[String, AttributeValue]): Either[CodecsUtils.DynamoDecoderException, T] = decoder.decode(item)

    override def flatMap[T1](f: T => D4SDecoder[T1]): D4SDecoder[T1]                  = decoder.flatMap(f)
    override def map[T1](f: T => T1): D4SDecoder[T1]                                  = decoder.map(f)
    override def map2[T1, A](another: D4SDecoder[T1])(f: (T, T1) => A): D4SDecoder[A] = decoder.map2(another)(f)

    override def contramap[T1](f: T1 => T): D4SEncoder[T1]                                  = encoder.contramap(f)
    override def contramap2[T1, A](another: D4SEncoder[T1])(f: A => (T, T1)): D4SEncoder[A] = encoder.contramap2(another)(f)

    def imap[B](to: T => B)(from: B => T): D4SCodec[B]                                     = fromPair(encoder.contramap(from), decoder.map(to))
    def imap2[B, C](another: D4SCodec[B])(to: (T, B) => C)(from: C => (T, B)): D4SCodec[C] = fromPair(encoder.contramap2(another)(from), decoder.map2(another)(to))
  }

  def derive[A](implicit derivedCodec: DerivationDerivedCodec[A]): D4SCodec[A] = D4SCodec.fromPair(derivedCodec.enc, derivedCodec.dec)
}
