package d4s.codecs

import java.util
import java.util.UUID

import cats.syntax.either._
import d4s.codecs.CodecsUtils.{CannotDecodeAttributeValue, DynamoDecoderException}
import magnolia._
import software.amazon.awssdk.core.SdkBytes
import software.amazon.awssdk.services.dynamodb.model.AttributeValue

import scala.collection.compat._
import scala.collection.mutable
import scala.jdk.CollectionConverters._
import scala.language.experimental.macros
import scala.util.Try

trait D4SDecoder[T] {
  self =>
  def decodeAttribute(attr: AttributeValue): Either[DynamoDecoderException, T]
  def decode(item: Map[String, AttributeValue]): Either[DynamoDecoderException, T]           = decode(item.asJava)
  def decode(item: java.util.Map[String, AttributeValue]): Either[DynamoDecoderException, T] = decodeAttribute(AttributeValue.builder().m(item).build())

  final def flatMap[T1](f: T => D4SDecoder[T1]): D4SDecoder[T1]                    = attr => self.decodeAttribute(attr).map(f).flatMap(_.decodeAttribute(attr))
  final def map[T1](f: T => T1): D4SDecoder[T1]                                    = attr => decodeAttribute(attr).map(f)
  final def map2[T1, A](another: D4SDecoder[T1])(f: (T, T1) => A): D4SDecoder[A]   = flatMap(t => another.map(t1 => f(t, t1)))
  final def contramapAttribute(f: AttributeValue => AttributeValue): D4SDecoder[T] = attr => decodeAttribute(f(attr))
  def contramapObject(f: Map[String, AttributeValue] => Map[String, AttributeValue]): D4SDecoder[T] = {
    attr =>
      val newAttr = Option(attr.m()).fold(attr)(m => AttributeValue.builder().m(f(m.asScala.toMap).asJava).build())
      decodeAttribute(newAttr)
  }
}

object D4SDecoder {
  def apply[A: D4SDecoder]: D4SDecoder[A] = implicitly
  def derived[T]: D4SDecoder[T] = macro Magnolia.gen[T]

  def decode[A: D4SDecoder](item: Map[String, AttributeValue]): Either[DynamoDecoderException, A]           = D4SDecoder[A].decode(item)
  def decode[A: D4SDecoder](item: java.util.Map[String, AttributeValue]): Either[DynamoDecoderException, A] = D4SDecoder[A].decode(item)
  def decodeAttribute[A: D4SDecoder](v: AttributeValue): Either[DynamoDecoderException, A]                  = D4SDecoder[A].decodeAttribute(v)

  def attributeDecoder[T](attributeDecoder: AttributeValue => Either[DynamoDecoderException, T]): D4SDecoder[T] = attributeDecoder(_)

  def objectDecoder[T](objectDecoder: Map[String, AttributeValue] => Either[DynamoDecoderException, T]): D4SDecoder[T] = new D4SDecoder[T] {
    override def decode(item: Map[String, AttributeValue]): Either[DynamoDecoderException, T]      = objectDecoder(item)
    override def decode(item: util.Map[String, AttributeValue]): Either[DynamoDecoderException, T] = decode(item.asScala.toMap)
    override def decodeAttribute(attr: AttributeValue): Either[DynamoDecoderException, T] = {
      Option(attr.m())
        .toRight(new CannotDecodeAttributeValue(s"Couldn't decode dynamo item=$attr as object. Does not have an `M` attribute (not a JSON object)", None))
        .flatMap(decode)
    }
    override def contramapObject(f: Map[String, AttributeValue] => Map[String, AttributeValue]): D4SDecoder[T] = {
      D4SDecoder.objectDecoder(objectDecoder apply f(_))
    }
  }

  /** Magnolia instances. */
  type Typeclass[T] = D4SDecoder[T]
  def combine[T](ctx: CaseClass[D4SDecoder, T]): D4SDecoder[T] = objectDecoder {
    item =>
      ctx.constructMonadic {
        p =>
          item.get(p.label) match {
            case Some(value) => p.typeclass.decodeAttribute(value)
            case None        => Left(new CannotDecodeAttributeValue(s"Cannot find parameter with name ${p.label}", None))
          }
      }
  }
  def dispatch[T](ctx: SealedTrait[D4SDecoder, T]): D4SDecoder[T] = attributeDecoder {
    item =>
      if (item.m().isEmpty) {
        ctx.subtypes
          .find(_.typeName.short == item.s())
          .toRight(new CannotDecodeAttributeValue(s" Cannot decode item of type ${ctx.typeName.full} from string: ${item.s()}", None))
          .flatMap(_.typeclass.decodeAttribute(item))
      } else {
        if (item.m().size != 1) {
          Left(new CannotDecodeAttributeValue("Invalid format of the encoded value", None))
        } else {
          val (typeName, attrValue) = item.m().asScala.head
          ctx.subtypes
            .find(_.typeName.short == typeName)
            .toRight(new CannotDecodeAttributeValue(s"Cannot find a subtype $typeName for a sealed trait ${ctx.typeName.full}", None))
            .flatMap(_.typeclass.decodeAttribute(attrValue))
        }
      }
  }

  implicit val attributeDecoder: D4SDecoder[AttributeValue] = Right(_)
  implicit val stringDecoder: D4SDecoder[String] = {
    attr =>
      Either.fromOption(Option(attr.s()), new CannotDecodeAttributeValue(s"Cannot decode $attr as String.", None))
  }
  implicit val byteDecoder: D4SDecoder[Byte] = {
    attr =>
      Either.fromTry(Try(attr.n().toByte)).leftMap(err => new CannotDecodeAttributeValue(s"Cannot decode $attr as Byte.", Some(err)))
  }
  implicit val shortDecoder: D4SDecoder[Short] = {
    attr =>
      Either.fromTry(Try(attr.n().toShort)).leftMap(err => new CannotDecodeAttributeValue(s"Cannot decode $attr as Short.", Some(err)))
  }
  implicit val intDecoder: D4SDecoder[Int] = {
    attr =>
      Either.fromTry(Try(attr.n().toInt)).leftMap(err => new CannotDecodeAttributeValue(s"Cannot decode $attr as Int.", Some(err)))
  }
  implicit val longDecoder: D4SDecoder[Long] = {
    attr =>
      Either.fromTry(Try(attr.n().toLong)).leftMap(err => new CannotDecodeAttributeValue(s"Cannot decode $attr as Long.", Some(err)))
  }
  implicit val doubleDecoder: D4SDecoder[Double] = {
    attr =>
      Either.fromTry(Try(attr.n().toDouble)).leftMap(err => new CannotDecodeAttributeValue(s"Cannot decode $attr as Double.", Some(err)))
  }
  implicit val boolDecoder: D4SDecoder[Boolean] = {
    attr =>
      Either.fromTry(Try(attr.bool().booleanValue())).leftMap(err => new CannotDecodeAttributeValue(s"Cannot decode $attr as Boolean.", Some(err)))
  }
  implicit val unitDecoder: D4SDecoder[Unit] = {
    attr =>
      if (attr.m().isEmpty) Right(()) else Left(new CannotDecodeAttributeValue(s"Cannot decode $attr as Unit.", None))
  }
  implicit val uuidDecoder: D4SDecoder[UUID] = {
    attr =>
      Either.fromTry(Try(UUID.fromString(attr.s()))).leftMap(err => new CannotDecodeAttributeValue(s"Cannot decode $attr as UUID", Some(err)))
  }
  implicit val sdkBytesDecoder: D4SDecoder[SdkBytes] = {
    attr =>
      Either.fromOption(Option(attr.b()), new CannotDecodeAttributeValue(s"Cannot decode $attr as SdkBytes", None))
  }
  implicit val arrayBytesDecoder: D4SDecoder[Array[Byte]] = {
    attr =>
      Either.fromTry(Try(attr.b().asByteArray())).leftMap(err => new CannotDecodeAttributeValue(s"Cannot decode $attr as Array[Byte]", Some(err)))
  }
  implicit val binarySetDecoded: D4SDecoder[Set[Array[Byte]]] = {
    attr =>
      Either
        .fromOption(
          Option(attr.bs()),
          new CannotDecodeAttributeValue(s"Cannot decode $attr as Set[Array[Byte]]", None)
        ).map(_.asScala.map(_.asByteArray()).toSet)
  }
  implicit def iterableDecoder[T, C[_] <: Iterable[T]](implicit T: D4SDecoder[T], factory: Factory[T, C[T]]): D4SDecoder[C[T]] = {
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

  implicit def mapLikeDecoder[K, V, M[k, v] <: Map[K, V]](implicit V: D4SDecoder[V], K: D4SKeyDecoder[K], factory: Factory[(K, V), M[K, V]]): D4SDecoder[M[K, V]] = {
    attributeDecoder {
      attr =>
        Either.fromTry(Try(attr.m())) match {
          case Left(error) => Left(new CannotDecodeAttributeValue(s"Cannot decode $attr as Map", Some(error)))
          case Right(value) =>
            value.asScala.iterator
              .foldLeft[Either[DynamoDecoderException, mutable.Builder[(K, V), M[K, V]]]](Right(factory.newBuilder)) {
                case (acc, (key, value)) =>
                  (K.decode(key), V.decodeAttribute(value)) match {
                    case (Right(k), Right(v))         => acc.map(_ ++= Iterable(k -> v))
                    case (Left(error), Right(_))      => Left(error)
                    case (Right(_), Left(error))      => Left(error)
                    case (Left(error1), Left(error2)) => Left(error1 union error2)
                  }
              }.map(_.result())

        }
    }
  }

  implicit def optionDecoder[T](implicit T: D4SDecoder[T]): D4SDecoder[Option[T]] = {
    attr: AttributeValue =>
      if (attr.nul()) Right(None) else T.decodeAttribute(attr).map(Some(_))
  }

  implicit def eitherDecoder[A: D4SDecoder, B: D4SDecoder]: D4SDecoder[Either[A, B]] = D4SDecoder.derived
}
