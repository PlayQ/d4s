package d4s.codecs

import java.util
import java.util.UUID

import cats.syntax.either._
import cats.syntax.foldable._
import d4s.models.DynamoException.DecoderException
import magnolia.{CaseClass, Magnolia, SealedTrait}
import software.amazon.awssdk.core.SdkBytes
import software.amazon.awssdk.core.util.{DefaultSdkAutoConstructList, DefaultSdkAutoConstructMap}
import software.amazon.awssdk.services.dynamodb.model.AttributeValue

import scala.collection.compat._
import scala.collection.mutable
import scala.jdk.CollectionConverters._
import scala.language.experimental.macros
import scala.util.Try

trait D4SDecoder[T] {
  def decodeAttribute(attr: AttributeValue): Either[DecoderException, T]
  def decode(item: Map[String, AttributeValue]): Either[DecoderException, T]           = decode(item.asJava)
  def decode(item: java.util.Map[String, AttributeValue]): Either[DecoderException, T] = decodeAttribute(AttributeValue.builder().m(item).build())

  final def flatMap[T1](f: T => D4SDecoder[T1]): D4SDecoder[T1]                    = attr => decodeAttribute(attr).map(f).flatMap(_.decodeAttribute(attr))
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
  @inline def apply[A: D4SDecoder]: D4SDecoder[A] = implicitly
  def derived[T]: D4SDecoder[T] = macro Magnolia.gen[T]

  def decode[A: D4SDecoder](item: Map[String, AttributeValue]): Either[DecoderException, A]           = D4SDecoder[A].decode(item)
  def decode[A: D4SDecoder](item: java.util.Map[String, AttributeValue]): Either[DecoderException, A] = D4SDecoder[A].decode(item)
  def decodeAttribute[A: D4SDecoder](v: AttributeValue): Either[DecoderException, A]                  = D4SDecoder[A].decodeAttribute(v)

  def attributeDecoder[T](attributeDecoder: AttributeValue => Either[DecoderException, T]): D4SDecoder[T] = attributeDecoder(_)

  def objectDecoder[T](objectDecoder: Map[String, AttributeValue] => Either[DecoderException, T]): D4SDecoder[T] = new D4SDecoder[T] {
    override def decode(item: Map[String, AttributeValue]): Either[DecoderException, T]      = objectDecoder(item)
    override def decode(item: util.Map[String, AttributeValue]): Either[DecoderException, T] = decode(item.asScala.toMap)
    override def decodeAttribute(attr: AttributeValue): Either[DecoderException, T] = {
      Option(attr.m())
        .toRight(DecoderException(s"Couldn't decode dynamo item=$attr as object. Does not have an `M` attribute (not a JSON object)", None))
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
            case None        => Left(DecoderException(s"Cannot find parameter with name ${p.label}", None))
          }
      }
  }
  def dispatch[T](ctx: SealedTrait[D4SDecoder, T]): D4SDecoder[T] = attributeDecoder {
    item =>
      if (item.m().isEmpty) {
        ctx.subtypes
          .find(_.typeName.short == item.s())
          .toRight(DecoderException(s" Cannot decode item of type ${ctx.typeName.full} from string: ${item.s()}", None))
          .flatMap(_.typeclass.decodeAttribute(item))
      } else {
        if (item.m().size != 1) {
          Left(DecoderException("Invalid format of the encoded value", None))
        } else {
          val (typeName, attrValue) = item.m().asScala.head
          ctx.subtypes
            .find(_.typeName.short == typeName)
            .toRight(DecoderException(s"Cannot find a subtype $typeName for a sealed trait ${ctx.typeName.full}", None))
            .flatMap(_.typeclass.decodeAttribute(attrValue))
        }
      }
  }

  implicit val attributeDecoder: D4SDecoder[AttributeValue] = Right(_)
  implicit val stringDecoder: D4SDecoder[String] = {
    attr =>
      Either.fromOption(Option(attr.s()), DecoderException(s"Cannot decode $attr as String", None))
  }
  implicit val byteDecoder: D4SDecoder[Byte]              = tryDecoder("Byte")(_.n().toByte)
  implicit val shortDecoder: D4SDecoder[Short]            = tryDecoder("Short")(_.n().toShort)
  implicit val intDecoder: D4SDecoder[Int]                = tryDecoder("Int")(_.n().toInt)
  implicit val longDecoder: D4SDecoder[Long]              = tryDecoder("Long")(_.n().toLong)
  implicit val floatDecoder: D4SDecoder[Float]            = tryDecoder("Float")(_.n().toFloat)
  implicit val doubleDecoder: D4SDecoder[Double]          = tryDecoder("Double")(_.n().toDouble)
  implicit val boolDecoder: D4SDecoder[Boolean]           = tryDecoder("Boolean")(_.bool().booleanValue())
  implicit val uuidDecoder: D4SDecoder[UUID]              = tryDecoder("UUID")(UUID fromString _.s())
  implicit val arrayBytesDecoder: D4SDecoder[Array[Byte]] = tryDecoder("Array[Byte]")(_.b().asByteArray())

  implicit val unitDecoder: D4SDecoder[Unit] = {
    attr =>
      if (attr.m().isEmpty) Right(()) else Left(DecoderException(s"Cannot decode $attr as Unit", None))
  }
  implicit val sdkBytesDecoder: D4SDecoder[SdkBytes] = {
    attr =>
      Either.fromOption(Option(attr.b()), DecoderException(s"Cannot decode $attr as SdkBytes", None))
  }
  implicit val binarySetSdkBytesDecoder: D4SDecoder[Set[SdkBytes]] = {
    attr =>
      Either
        .fromOption(
          Option(attr.bs()).filter(!_.isInstanceOf[DefaultSdkAutoConstructList[_]]),
          DecoderException(s"Cannot decode $attr as Set[SdkBytes]", None)
        ).map(_.asScala.toSet)
  }
  implicit val binarySetDecoder: D4SDecoder[Set[Array[Byte]]] = binarySetSdkBytesDecoder.map(_.map(_.asByteArray()))

  implicit val stringSetDecoder: D4SDecoder[Set[String]] = {
    attr =>
      Either
        .fromOption(
          Option(attr.ss()).filter(!_.isInstanceOf[DefaultSdkAutoConstructList[_]]),
          DecoderException(s"Cannot decode $attr as Set[String]", None)
        ).map(_.asScala.toSet)
  }

  implicit val byteSetDecoder: D4SDecoder[Set[Byte]]     = numberSetDecoder("Byte")(_.toByte)
  implicit val shortSetDecoder: D4SDecoder[Set[Short]]   = numberSetDecoder("Short")(_.toShort)
  implicit val intSetDecoder: D4SDecoder[Set[Int]]       = numberSetDecoder("Int")(_.toInt)
  implicit val longSetDecoder: D4SDecoder[Set[Long]]     = numberSetDecoder("Long")(_.toLong)
  implicit val floatSetDecoder: D4SDecoder[Set[Float]]   = numberSetDecoder("Float")(_.toFloat)
  implicit val doubleSetDecoder: D4SDecoder[Set[Double]] = numberSetDecoder("Double")(_.toDouble)

  implicit def iterableDecoder[T, C[x] <: Iterable[x]](implicit T: D4SDecoder[T], factory: Factory[T, C[T]]): D4SDecoder[C[T]] = {
    attr =>
      Either.fromTry(Try(attr.l()).filter(!_.isInstanceOf[DefaultSdkAutoConstructList[_]])) match {
        case Left(err) => Left(DecoderException(s"Cannot decode $attr as List", Some(err)))
        case Right(value) =>
          value.asScala.toList
            .foldM[Either[DecoderException, ?], mutable.Builder[T, C[T]]](factory.newBuilder) {
              (acc, attr) =>
                T.decodeAttribute(attr).map(acc += _)
            }.map(_.result())
      }
  }

  implicit def mapLikeDecoder[K, V, M[k, v] <: Map[k, v]](implicit V: D4SDecoder[V], K: D4SKeyDecoder[K], factory: Factory[(K, V), M[K, V]]): D4SDecoder[M[K, V]] = {
    attributeDecoder {
      attr =>
        Either.fromTry(Try(attr.m()).filter(!_.isInstanceOf[DefaultSdkAutoConstructMap[_, _]])) match {
          case Left(err) => Left(DecoderException(s"Cannot decode $attr as Map", Some(err)))
          case Right(value) =>
            value.asScala.toList
              .foldM[Either[DecoderException, ?], mutable.Builder[(K, V), M[K, V]]](factory.newBuilder) {
                case (acc, (key, value)) =>
                  (K.decode(key), V.decodeAttribute(value)) match {
                    case (Right(k), Right(v))         => Right(acc ++= Iterable(k -> v))
                    case (Left(error), Right(_))      => Left(error)
                    case (Right(_), Left(error))      => Left(error)
                    case (Left(error1), Left(error2)) => Left(error1 union error2)
                  }
              }.map(_.result())
        }
    }
  }

  implicit def optionDecoder[T](implicit T: D4SDecoder[T]): D4SDecoder[Option[T]] = {
    attr =>
      if (attr.nul()) Right(None) else T.decodeAttribute(attr).map(Some(_))
  }

  implicit def eitherDecoder[A: D4SDecoder, B: D4SDecoder]: D4SDecoder[Either[A, B]] = D4SDecoder.derived

  def tryDecoder[A](name: String)(f: AttributeValue => A): D4SDecoder[A] = {
    attr =>
      Either.fromTry(Try(f(attr))).leftMap(err => DecoderException(s"Cannot decode $attr as $name", Some(err)))
  }

  def numberSetDecoder[N](name: String)(f: String => N): D4SDecoder[Set[N]] = {
    attr =>
      Either.fromTry {
        Try(attr.ns())
          .filter(!_.isInstanceOf[DefaultSdkAutoConstructList[_]])
          .map(_.asScala.map(f).toSet)
      }.leftMap(err => DecoderException(s"Cannot decode $attr as Set[$name]", Some(err)))
  }

}
