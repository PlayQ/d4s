package d4s.codecs

import software.amazon.awssdk.services.dynamodb.model.AttributeValue

trait D4SCodec[A] extends D4SEncoder[A] with D4SDecoder[A]

object D4SCodec {
  def apply[A: D4SCodec]: D4SCodec[A] = implicitly

  def derive[A](implicit derivedCodec: DerivationDerivedCodec[A]): D4SCodec[A] = new D4SCodec[A] {
    override def encode(item: A): Map[String, AttributeValue]                                             = derivedCodec.enc.encode(item)
    override def decode(item: Map[String, AttributeValue]): Either[CodecsUtils.DynamoDecoderException, A] = derivedCodec.dec.decode(item)
  }

}
