package d4s.codecs

import d4s.codecs.CodecsUtils.{CannotDecodeAttributeValue, CastedMagnolia, DynamoDecoderException}
import magnolia._
import software.amazon.awssdk.services.dynamodb.model.AttributeValue

import scala.jdk.CollectionConverters._
import scala.language.experimental.macros

trait D4SDecoder[T] extends D4SAttributeDecoder[T] {
  self =>
  def decode(item: Map[String, AttributeValue]): Either[DynamoDecoderException, T]
  def decode(item: java.util.Map[String, AttributeValue]): Either[DynamoDecoderException, T] = decode(item.asScala.toMap)

  def decodeAttribute(item: AttributeValue): Either[DynamoDecoderException, T] = {
    Option(item.m())
      .toRight(new CannotDecodeAttributeValue(s"Couldn't decode dynamo item=$item as object. Does not have an `M` attribute (not a JSON object)", None))
      .flatMap(decode)
  }

  def flatMap[T1](f: T => D4SDecoder[T1]): D4SDecoder[T1]                  = item => self.decode(item).map(f).flatMap(_.decode(item))
  def map[T1](f: T => T1): D4SDecoder[T1]                                  = item => decode(item).map(f)
  def map2[T1, A](another: D4SDecoder[T1])(f: (T, T1) => A): D4SDecoder[A] = flatMap(t => another.map(t1 => f(t, t1)))
}

object D4SDecoder {
  def apply[A: D4SDecoder]: D4SDecoder[A] = implicitly
  def derived[T]: D4SDecoder[T] = macro CastedMagnolia.genWithCast[T, D4SDecoder[_]]

  def decode[A: D4SDecoder](item: Map[String, AttributeValue]): Either[DynamoDecoderException, A]           = D4SDecoder[A].decode(item)
  def decode[A: D4SDecoder](item: java.util.Map[String, AttributeValue]): Either[DynamoDecoderException, A] = D4SDecoder[A].decode(item)
  def decodeAttribute[A: D4SAttributeDecoder](v: AttributeValue): Either[DynamoDecoderException, A]         = D4SAttributeDecoder[A].decodeAttribute(v)

  implicit def fromCodec[T](implicit self: D4SCodec[T]): D4SDecoder[T] = self.decoder

  /** Magnolia instances. */
  type Typeclass[T] = D4SAttributeDecoder[T]
  def combine[T](ctx: CaseClass[D4SAttributeDecoder, T]): D4SDecoder[T] = {
    item =>
      ctx.constructMonadic {
        p =>
          item.get(p.label) match {
            case Some(value) => p.typeclass.decodeAttribute(value)
            case None        => Left(new CannotDecodeAttributeValue(s"Cannot find parameter with name ${p.label}", None))
          }
      }
  }
  def dispatch[T](ctx: SealedTrait[D4SDecoder, T]): D4SDecoder[T] = {
    item =>
      import cats.implicits._
      ctx.subtypes.toList
        .collectFirstSome(_.typeclass.decode(item).toOption)
        .toRight(new CannotDecodeAttributeValue(s"Cannot decode item of type ${ctx.typeName}.", None))
  }
}
