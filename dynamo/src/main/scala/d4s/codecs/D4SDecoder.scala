package d4s.codecs

import cats.syntax.either._
import d4s.codecs.CodecsUtils.{CannotDecodeAttributeValue, CastedMagnolia, DynamoDecoderException}
import magnolia._
import software.amazon.awssdk.services.dynamodb.model.AttributeValue

import scala.collection.compat._
import scala.collection.mutable
import scala.jdk.CollectionConverters._
import scala.language.experimental.macros
import scala.util.Try

trait D4SAttributeDecoder[T] {
  def decodeAttribute(item: AttributeValue): Either[DynamoDecoderException, T]
}

object D4SAttributeDecoder {
  def apply[T: D4SAttributeDecoder]: D4SAttributeDecoder[T] = implicitly

  def decodeAttributeValue[T: D4SAttributeDecoder](item: AttributeValue): Either[Throwable, T] = D4SAttributeDecoder[T].decodeAttribute(item)

  implicit val stringDecoder: D4SAttributeDecoder[String] = attr =>
    Either.fromOption(Option(attr.s()), new CannotDecodeAttributeValue(s"Cannot decode $attr as String.", None))

  implicit val intDecoder: D4SAttributeDecoder[Int] = attr =>
    Either.fromTry(Try(attr.n().toInt)).leftMap(err => new CannotDecodeAttributeValue(s"Cannot decode $attr as Int.", Some(err)))

  implicit val longDecoder: D4SAttributeDecoder[Long] = attr =>
    Either.fromTry(Try(attr.n().toLong)).leftMap(err => new CannotDecodeAttributeValue(s"Cannot decode $attr as Long.", Some(err)))

  implicit val doubleDecoder: D4SAttributeDecoder[Double] = attr =>
    Either.fromTry(Try(attr.n().toDouble)).leftMap(err => new CannotDecodeAttributeValue(s"Cannot decode $attr as Double.", Some(err)))

  implicit val boolDecoder: D4SAttributeDecoder[Boolean] = attr =>
    Either.fromTry(Try(attr.bool().booleanValue())).leftMap(err => new CannotDecodeAttributeValue(s"Cannot decode $attr as Boolean.", Some(err)))

  implicit val unitDecoder: D4SAttributeDecoder[Unit] = attr =>
    if (attr.m().isEmpty) Right(()) else Left(new CannotDecodeAttributeValue(s"Cannot decode $attr as Boolean.", None))

  implicit val sdkBytesDecoder: D4SAttributeDecoder[Array[Byte]] = attr =>
    Either.fromTry(Try(attr.b().asByteArray())).leftMap(err => new CannotDecodeAttributeValue(s"Cannot decode $attr as Array[Byte]", Some(err)))

  implicit val binarySetDecoded: D4SAttributeDecoder[Set[Array[Byte]]] = attr =>
    Either
      .fromOption(
        Option(attr.bs()),
        new CannotDecodeAttributeValue(s"Cannot decode $attr as Set[Array[Byte]]", None)
      ).map(_.asScala.map(_.asByteArray()).toSet)

  implicit def iterableDecoder[T, C[_] <: Iterable[T]](implicit T: D4SAttributeDecoder[T], factory: Factory[T, C[T]]): D4SAttributeDecoder[C[T]] = {
    attr: AttributeValue =>
      Either.fromTry(Try(attr.l())) match {
        case Left(error) => Left(new CannotDecodeAttributeValue(s"Cannot decode $attr as List", Some(error)))
        case Right(value) =>
          value.asScala.iterator
            .foldLeft[Either[DynamoDecoderException, mutable.Builder[T, C[T]]]](Right(factory.newBuilder)) {
              (acc, attr) =>
                T.decodeAttribute(attr) match {
                  case Left(err)    => Left(err)
                  case Right(value) => acc.map(_ += value)
                }
            }.map(_.result())
      }
  }

  implicit def mapLikeDecoder[V, M[_, _] <: Map[String, V]](
    implicit
    V: D4SAttributeDecoder[V],
    factory: Factory[(String, V), M[String, V]]
  ): D4SAttributeDecoder[M[String, V]] = {
    attr =>
      Either.fromTry(Try(attr.m())) match {
        case Left(error) => Left(new CannotDecodeAttributeValue(s"Cannot decode $attr as Map", Some(error)))
        case Right(value) =>
          value.asScala.iterator
            .foldLeft[Either[DynamoDecoderException, mutable.Builder[(String, V), M[String, V]]]](Right(factory.newBuilder)) {
              case (acc, (name, attr)) =>
                V.decodeAttribute(attr) match {
                  case Left(error) => Left(error)
                  case Right(value) =>
                    acc.map(_ ++= Iterable((name -> value)))
                }
            }.map(_.result())
      }
  }

  implicit def optionDecoder[T](implicit T: D4SAttributeDecoder[T]): D4SAttributeDecoder[Option[T]] = {
    attr: AttributeValue =>
      if (attr.nul()) Right(None) else T.decodeAttribute(attr).map(Some(_))
  }
}

trait D4SDecoder[T] extends D4SAttributeDecoder[T] {
  def decode(item: Map[String, AttributeValue]): Either[DynamoDecoderException, T]
  def decode(item: java.util.Map[String, AttributeValue]): Either[DynamoDecoderException, T] = decode(item.asScala.toMap)
}

object D4SDecoder {
  def apply[A: D4SDecoder]: D4SDecoder[A] = implicitly

  def decode[A: D4SDecoder](item: Map[String, AttributeValue]): Either[DynamoDecoderException, A]           = D4SDecoder[A].decode(item)
  def decode[A: D4SDecoder](item: java.util.Map[String, AttributeValue]): Either[DynamoDecoderException, A] = D4SDecoder[A].decode(item)
  def decodeAttribute[A: D4SAttributeDecoder](v: AttributeValue): Either[DynamoDecoderException, A]         = D4SAttributeDecoder[A].decodeAttribute(v)

  type Typeclass[T] = D4SAttributeDecoder[T]
  def derived[T]: D4SDecoder[T] = macro CastedMagnolia.genWithCast[T, D4SDecoder[Nothing]]

  def combine[T](ctx: CaseClass[D4SAttributeDecoder, T]): D4SDecoder[T] = new D4SDecoder[T] {
    override def decode(item: Map[String, AttributeValue]): Either[DynamoDecoderException, T] = {
      ctx.constructMonadic {
        p =>
          item.get(p.label) match {
            case Some(value) => p.typeclass.decodeAttribute(value)
            case None => Left(new CannotDecodeAttributeValue(s"Cannot find parameter with name ${p.label}", None))
          }
      }
    }

    override def decodeAttribute(v: AttributeValue): Either[DynamoDecoderException, T] = ctx.constructMonadic(_.typeclass.decodeAttribute(v))
  }

  def dispatch[T](ctx: SealedTrait[D4SDecoder, T]): D4SDecoder[T] = new D4SDecoder[T] {
    override def decode(item: Map[String, AttributeValue]): Either[DynamoDecoderException, T] =
      ctx.subtypes.dropWhile(s => s.typeclass.decode(item).isLeft).head.typeclass.decode(item)

    override def decodeAttribute(v: AttributeValue): Either[DynamoDecoderException, T] =
      ctx.subtypes.dropWhile(s => s.typeclass.decodeAttribute(v).isLeft).head.typeclass.decodeAttribute(v)
  }
}
