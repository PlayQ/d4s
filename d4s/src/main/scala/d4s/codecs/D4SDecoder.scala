package d4s.codecs

import java.util.UUID

import cats.instances.either._
import cats.instances.list._
import cats.syntax.either._
import cats.syntax.foldable._
import d4s.codecs.D4SDecoder.attributeDecoder
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
import scala.util.control.NonFatal

trait D4SDecoder[T] {
  def decode(attr: AttributeValue): Either[DecoderException, T]
  def decodeObject(item: Map[String, AttributeValue]): Either[DecoderException, T]
  def decodeOptional(attr: Option[AttributeValue], label: String): Either[DecoderException, T] = {
    attr match {
      case Some(value) => decode(value)
      case None        => Left(DecoderException(s"Cannot find parameter with name `$label` of type [${this.getClass.getSimpleName}].", None))
    }
  }

  final def decodeObject(item: java.util.Map[String, AttributeValue]): Either[DecoderException, T] = decodeObject(item.asScala.toMap)

  final def flatMap[T1](f: T => D4SDecoder[T1]): D4SDecoder[T1]                            = attributeDecoder(attr => decode(attr).map(f).flatMap(_.decode(attr)))
  final def map[T1](f: T => T1): D4SDecoder[T1]                                            = attributeDecoder(attr => decode(attr).map(f))
  final def map2[T1, A](another: D4SDecoder[T1])(f: (T, T1) => A): D4SDecoder[A]           = flatMap(t => another.map(t1 => f(t, t1)))
  final def preprocessAttributeDecoder(f: AttributeValue => AttributeValue): D4SDecoder[T] = attributeDecoder(attr => decode(f(attr)))
  def preprocessObjectDecoder(f: Map[String, AttributeValue] => Map[String, AttributeValue]): D4SDecoder[T] = attributeDecoder {
    attr =>
      val newAttr = Option(attr.m()).fold(attr)(m => AttributeValue.builder().m(f(m.asScala.toMap).asJava).build())
      decode(newAttr)
  }
}

object D4SDecoder extends D4SDecoderScala213 {
  @inline def apply[A](implicit ev: D4SDecoder[A]): ev.type = ev

  def derived[A]: D4SDecoder[A] = macro Magnolia.gen[A]

  def decode[A: D4SDecoder](attr: AttributeValue): Either[DecoderException, A]                              = D4SDecoder[A].decode(attr)
  def decodeObject[A: D4SDecoder](item: Map[String, AttributeValue]): Either[DecoderException, A]           = D4SDecoder[A].decodeObject(item)
  def decodeObject[A: D4SDecoder](item: java.util.Map[String, AttributeValue]): Either[DecoderException, A] = D4SDecoder[A].decodeObject(item)
  def decodeField[A: D4SDecoder](key: String, item: Map[String, AttributeValue]): Either[DecoderException, A] = {
    if (item.size != 1) {
      Left(DecoderException(s"Invalid format when decoding a single element map with key `$key` - attribute map size is not 1, attribute map: $item", None))
    } else {
      val (typeName, attrValue) = item.head
      if (typeName != key) {
        Left(DecoderException(s"Invalid format when decoding a single element map with key `$key` - key is different `$typeName`", None))
      } else decode[A](attrValue)
    }
  }

  def attributeDecoder[T](attributeDecoder: AttributeValue => Either[DecoderException, T]): D4SDecoder[T] = new D4SDecoder[T] {
    override def decode(attr: AttributeValue): Either[DecoderException, T]                    = attributeDecoder(attr)
    override def decodeObject(item: Map[String, AttributeValue]): Either[DecoderException, T] = attributeDecoder(AttributeValue.builder().m(item.asJava).build())
  }

  def objectDecoder[T](objectDecoder: Map[String, AttributeValue] => Either[DecoderException, T]): D4SDecoder[T] = new D4SDecoder[T] {
    override def decodeObject(item: Map[String, AttributeValue]): Either[DecoderException, T] = objectDecoder(item)
    override def decode(attr: AttributeValue): Either[DecoderException, T] = {
      Option(attr.m())
        .toRight(DecoderException(s"Couldn't decode dynamo item=$attr as object. Does not have an `M` attribute (not a JSON object)", None))
        .flatMap(decodeObject)
    }
    override def preprocessObjectDecoder(f: Map[String, AttributeValue] => Map[String, AttributeValue]): D4SDecoder[T] = {
      D4SDecoder.objectDecoder(objectDecoder apply f(_))
    }
  }

  /** Magnolia instances. */
  private[D4SDecoder] type Typeclass[T] = D4SDecoder[T]

  def combine[T](ctx: CaseClass[D4SDecoder, T]): D4SDecoder[T] = objectDecoder {
    item =>
      ctx.constructMonadic {
        p =>
          val label = p.label
          p.typeclass.decodeOptional(item.get(label), label)
      }
  }

  def dispatch[T](ctx: SealedTrait[D4SDecoder, T]): D4SDecoder[T] = attributeDecoder {
    item =>
      if (item.m().isEmpty) {
        ctx.subtypes
          .find(_.typeName.short == item.s())
          .toRight(DecoderException(s"Cannot decode item of type [${ctx.typeName.full}] from string: ${item.s()}", None))
          .flatMap(_.typeclass.decode(item))
      } else {
        if (item.m().size != 1) {
          Left(DecoderException(s"Invalid format when decoding a sealed trait - attribute map size is not 1, attribute map: ${item.m().asScala}", None))
        } else {
          val (typeName, attrValue) = item.m().asScala.head
          ctx.subtypes
            .find(_.typeName.short == typeName)
            .toRight(DecoderException(s"Cannot find a subtype [$typeName] for a sealed trait [${ctx.typeName.full}]", None))
            .flatMap(_.typeclass.decode(attrValue))
        }
      }
  }

  implicit val attributeDecoder: D4SDecoder[AttributeValue] = attributeDecoder(Right(_))
  implicit val stringDecoder: D4SDecoder[String] = attributeDecoder {
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

  implicit val unitDecoder: D4SDecoder[Unit] = attributeDecoder {
    attr =>
      if (attr.m().isEmpty) Right(()) else Left(DecoderException(s"Cannot decode $attr as Unit", None))
  }
  implicit val sdkBytesDecoder: D4SDecoder[SdkBytes] = attributeDecoder {
    attr =>
      Either.fromOption(Option(attr.b()), DecoderException(s"Cannot decode $attr as SdkBytes", None))
  }
  implicit val binarySetSdkBytesDecoder: D4SDecoder[Set[SdkBytes]] = attributeDecoder {
    attr =>
      Either
        .fromOption(
          Option(attr.bs()).filter(!_.isInstanceOf[DefaultSdkAutoConstructList[_]]),
          DecoderException(s"Cannot decode $attr as Set[SdkBytes]", None),
        ).map(_.asScala.toSet)
  }
  implicit val binarySetDecoder: D4SDecoder[Set[Array[Byte]]] = binarySetSdkBytesDecoder.map(_.map(_.asByteArray()))

  implicit val stringSetDecoder: D4SDecoder[Set[String]] = attributeDecoder {
    attr =>
      Either
        .fromOption(
          Option(attr.ss()).filter(!_.isInstanceOf[DefaultSdkAutoConstructList[_]]),
          DecoderException(s"Cannot decode $attr as Set[String]", None),
        ).map(_.asScala.toSet)
  }

  implicit val byteSetDecoder: D4SDecoder[Set[Byte]]     = numberSetDecoder("Byte")(_.toByte)
  implicit val shortSetDecoder: D4SDecoder[Set[Short]]   = numberSetDecoder("Short")(_.toShort)
  implicit val intSetDecoder: D4SDecoder[Set[Int]]       = numberSetDecoder("Int")(_.toInt)
  implicit val longSetDecoder: D4SDecoder[Set[Long]]     = numberSetDecoder("Long")(_.toLong)
  implicit val floatSetDecoder: D4SDecoder[Set[Float]]   = numberSetDecoder("Float")(_.toFloat)
  implicit val doubleSetDecoder: D4SDecoder[Set[Double]] = numberSetDecoder("Double")(_.toDouble)

  implicit def iterableDecoder[A, C[x] <: Iterable[x]](implicit T: D4SDecoder[A], factory: Factory[A, C[A]]): D4SDecoder[C[A]] = attributeDecoder {
    attr =>
      Either.fromTry(Try(attr.l()).filter(!_.isInstanceOf[DefaultSdkAutoConstructList[_]])) match {
        case Left(err) => Left(DecoderException(s"Cannot decode $attr as List", Some(err)))
        case Right(value) =>
          value.asScala.toList
            .foldM[Either[DecoderException, ?], mutable.Builder[A, C[A]]](factory.newBuilder) {
              (acc, attr) =>
                T.decode(attr).map(acc += _)
            }.map(_.result())
      }
  }

  implicit def mapLikeDecoder[K, V, M[k, v] <: Map[k, v]](implicit V: D4SDecoder[V], K: D4SKeyDecoder[K], factory: Factory[(K, V), M[K, V]]): D4SDecoder[M[K, V]] =
    attributeDecoder {
      attr =>
        Either.fromTry(Try(attr.m()).filter(!_.isInstanceOf[DefaultSdkAutoConstructMap[_, _]])) match {
          case Left(err) => Left(DecoderException(s"Cannot decode $attr as Map", Some(err)))
          case Right(value) =>
            value.asScala.toList
              .foldM[Either[DecoderException, ?], mutable.Builder[(K, V), M[K, V]]](factory.newBuilder) {
                case (acc, (key, value)) =>
                  (K.decode(key), V.decode(value)) match {
                    case (Right(k), Right(v))         => Right(acc ++= Iterable(k -> v))
                    case (Left(error), Right(_))      => Left(error)
                    case (Right(_), Left(error))      => Left(error)
                    case (Left(error1), Left(error2)) => Left(error1 union error2)
                  }
              }.map(_.result())
        }
    }

  implicit def optionDecoder[A](implicit T: D4SDecoder[A]): D4SDecoder[Option[A]] = new D4SDecoder[Option[A]] {
    override def decode(attr: AttributeValue): Either[DecoderException, Option[A]]                    = decodeOptional(Some(attr), "")
    override def decodeObject(item: Map[String, AttributeValue]): Either[DecoderException, Option[A]] = decode(AttributeValue.builder().m(item.asJava).build())
    override def decodeOptional(attr: Option[AttributeValue], label: String): Either[DecoderException, Option[A]] = attr match {
      case Some(attr) if attr.nul() => Right(None)
      case Some(attr)               => T.decode(attr).map(Some(_))
      case None                     => Right(None)
    }
  }

  implicit def eitherDecoder[A: D4SDecoder, B: D4SDecoder]: D4SDecoder[Either[A, B]] = D4SDecoder.derived

  def tryDecoder[A](name: String)(f: AttributeValue => A): D4SDecoder[A] = attributeDecoder {
    attr =>
      try {
        Right(f(attr))
      } catch {
        case err if NonFatal(err) =>
          Left(DecoderException(s"Cannot decode $attr as $name", Some(err)))
      }
  }

  def numberSetDecoder[N](name: String)(f: String => N): D4SDecoder[Set[N]] = attributeDecoder {
    attr =>
      Either.fromTry {
        Try(attr.ns())
          .filter(!_.isInstanceOf[DefaultSdkAutoConstructList[_]])
          .map(_.asScala.map(f).toSet)
      }.leftMap(err => DecoderException(s"Cannot decode $attr as Set[$name]", Some(err)))
  }

}
