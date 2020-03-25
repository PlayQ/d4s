package d4s.codecs

import java.util

import cats.syntax.either._
import d4s.codecs.CodecsUtils.{CannotDecodeAttributeValue, CastedMagnolia, DynamoDecoderException}
import magnolia._
import software.amazon.awssdk.services.dynamodb.model.AttributeValue

import scala.collection.compat._
import scala.collection.mutable
import scala.jdk.CollectionConverters._
import scala.language.experimental.macros
import scala.util.Try

trait D4SDecoder[T] {
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
  def attributeDecoder[T](attributeDecoder: AttributeValue => Either[DynamoDecoderException, T]): D4SDecoder[T] = new D4SDecoder[T] {
    override def decode(item: Map[String, AttributeValue]): Either[DynamoDecoderException, T] =
      Left(new CannotDecodeAttributeValue(s"Cannot decode map to a single value.", None))
    override def decode(item: util.Map[String, AttributeValue]): Either[DynamoDecoderException, T] =
      Left(new CannotDecodeAttributeValue(s"Cannot decode map to a single value.", None))
    override def decodeAttribute(item: AttributeValue): Either[DynamoDecoderException, T] = attributeDecoder(item)
  }

  def decode[A: D4SDecoder](item: Map[String, AttributeValue]): Either[DynamoDecoderException, A]           = D4SDecoder[A].decode(item)
  def decode[A: D4SDecoder](item: java.util.Map[String, AttributeValue]): Either[DynamoDecoderException, A] = D4SDecoder[A].decode(item)
  def decodeAttribute[A: D4SDecoder](v: AttributeValue): Either[DynamoDecoderException, A]                  = D4SDecoder[A].decodeAttribute(v)

  /** Magnolia instances. */
  type Typeclass[T] = D4SDecoder[T]
  def combine[T](ctx: CaseClass[D4SDecoder, T]): D4SDecoder[T] = {
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

  implicit val attributeDecoder: D4SDecoder[AttributeValue] = attributeDecoder(attr => Either.right(attr))
  implicit val stringDecoder: D4SDecoder[String] = attributeDecoder {
    attr =>
      Either.fromOption(Option(attr.s()), new CannotDecodeAttributeValue(s"Cannot decode $attr as String.", None))
  }
  implicit val intDecoder: D4SDecoder[Int] = attributeDecoder {
    attr =>
      Either.fromTry(Try(attr.n().toInt)).leftMap(err => new CannotDecodeAttributeValue(s"Cannot decode $attr as Int.", Some(err)))
  }
  implicit val longDecoder: D4SDecoder[Long] = attributeDecoder {
    attr =>
      Either.fromTry(Try(attr.n().toLong)).leftMap(err => new CannotDecodeAttributeValue(s"Cannot decode $attr as Long.", Some(err)))
  }
  implicit val doubleDecoder: D4SDecoder[Double] = attributeDecoder {
    attr =>
      Either.fromTry(Try(attr.n().toDouble)).leftMap(err => new CannotDecodeAttributeValue(s"Cannot decode $attr as Double.", Some(err)))
  }
  implicit val boolDecoder: D4SDecoder[Boolean] = attributeDecoder {
    attr =>
      Either.fromTry(Try(attr.bool().booleanValue())).leftMap(err => new CannotDecodeAttributeValue(s"Cannot decode $attr as Boolean.", Some(err)))
  }
  implicit val unitDecoder: D4SDecoder[Unit] = attributeDecoder {
    attr =>
      if (attr.m().isEmpty) Right(()) else Left(new CannotDecodeAttributeValue(s"Cannot decode $attr as Unit.", None))
  }
  implicit val sdkBytesDecoder: D4SDecoder[Array[Byte]] = attributeDecoder {
    attr =>
      Either.fromTry(Try(attr.b().asByteArray())).leftMap(err => new CannotDecodeAttributeValue(s"Cannot decode $attr as Array[Byte]", Some(err)))
  }
  implicit val binarySetDecoded: D4SDecoder[Set[Array[Byte]]] = attributeDecoder {
    attr =>
      Either
        .fromOption(
          Option(attr.bs()),
          new CannotDecodeAttributeValue(s"Cannot decode $attr as Set[Array[Byte]]", None)
        ).map(_.asScala.map(_.asByteArray()).toSet)
  }
  implicit def iterableDecoder[T, C[_] <: Iterable[T]](implicit T: D4SDecoder[T], factory: Factory[T, C[T]]): D4SDecoder[C[T]] = attributeDecoder {
    attr =>
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
  implicit def mapLikeDecoder[V, M[K, _] <: Map[K, V]](implicit V: D4SDecoder[V], factory: Factory[(String, V), M[String, V]]): D4SDecoder[M[String, V]] = {
    attributeDecoder {
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
  }
  implicit def optionDecoder[T](implicit T: D4SDecoder[T]): D4SDecoder[Option[T]] = attributeDecoder {
    attr: AttributeValue =>
      if (attr.nul()) Right(None) else T.decodeAttribute(attr).map(Some(_))
  }
}
