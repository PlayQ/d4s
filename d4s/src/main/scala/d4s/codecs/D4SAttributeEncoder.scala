package d4s.codecs

import java.util.UUID

import magnolia.{Magnolia, ReadOnlyCaseClass, SealedTrait}
import software.amazon.awssdk.core.SdkBytes
import software.amazon.awssdk.services.dynamodb.model.AttributeValue

import scala.jdk.CollectionConverters._
import scala.language.experimental.macros

trait D4SAttributeEncoder[T] {
  def encode(item: T): Option[AttributeValue]

  def contramap[T1](f: T1 => T): D4SAttributeEncoder[T1]                                       = item => encode(f(item))
  def postprocessAttributeEncoder(f: Option[AttributeValue] => Option[AttributeValue]): D4SAttributeEncoder[T] = item => f(encode(item))
}

object D4SAttributeEncoder {
  @inline def apply[A](implicit ev: D4SAttributeEncoder[A]): ev.type = ev

  def derived[T]: D4SAttributeEncoder[T] = macro Magnolia.gen[T]

  def encode[T: D4SAttributeEncoder](item: T): Option[AttributeValue]                             = D4SAttributeEncoder[T].encode(item)
  def encodeField[T: D4SAttributeEncoder](name: String, item: T): Map[String, AttributeValue] =
    D4SAttributeEncoder[T].encode(item) match {
      case Some(value) => Map(name -> value)
      case None => Map.empty
    }

  def traitEncoder[A](caseMap: A => (String, D4SAttributeEncoder[? <: A])): D4SAttributeEncoder[A] = {
    item =>
      val typeNameEncoder = caseMap(item)
      if (typeNameEncoder._2.isInstanceOf[CaseObjectEncoder[?]]) {
        typeNameEncoder._2.asInstanceOf[D4SAttributeEncoder[A]].encode(item)
      } else {
        Some(AttributeValue.builder().m((
          (typeNameEncoder._2.asInstanceOf[D4SAttributeEncoder[A]].encode(item)) match {
            case Some(value) => Map(typeNameEncoder._1 -> value)
            case None => Map.empty: Map[String, AttributeValue]
          }).asJava).build())
      }
  }

  /** Magnolia instances */
  private[D4SAttributeEncoder] type Typeclass[T] = D4SAttributeEncoder[T]

  def combine[T](ctx: ReadOnlyCaseClass[D4SAttributeEncoder, T]): D4SAttributeEncoder[T] = {
    if (ctx.isObject) {
      new CaseObjectEncoder[T](ctx.typeName.short)
    } else {
      item =>
        val map = ctx.parameters.map {
          p =>
            p.label -> p.typeclass.encode(p.dereference(item))
        }.toMap.filter(_._2.nonEmpty).view.mapValues(_.get).toMap
        Some(AttributeValue.builder().m(map.asJava).build())
    }
  }

  def dispatch[T](ctx: SealedTrait[D4SAttributeEncoder, T]): D4SAttributeEncoder[T] = {
    traitEncoder[T](ctx.dispatch(_)(subtype => subtype.typeName.short -> subtype.typeclass))
  }

  implicit val attributeEncoder: D4SAttributeEncoder[AttributeValue] = a => Some(a)
  implicit val stringEncoder: D4SAttributeEncoder[String]            = n => Some(AttributeValue.builder().s(n).build())
  implicit val boolEncoder: D4SAttributeEncoder[Boolean]             = n => Some(AttributeValue.builder().bool(n).build())
  implicit val byteEncoder: D4SAttributeEncoder[Byte]                = numericAttributeEncoder[Byte]
  implicit val shortEncoder: D4SAttributeEncoder[Short]              = numericAttributeEncoder[Short]
  implicit val intEncoder: D4SAttributeEncoder[Int]                  = numericAttributeEncoder[Int]
  implicit val longEncoder: D4SAttributeEncoder[Long]                = numericAttributeEncoder[Long]
  implicit val floatEncoder: D4SAttributeEncoder[Float]              = numericAttributeEncoder[Float]
  implicit val doubleEncoder: D4SAttributeEncoder[Double]            = numericAttributeEncoder[Double]
  implicit val unitEncoder: D4SAttributeEncoder[Unit]                = _ => Some(AttributeValue.builder().m(Map.empty[String, AttributeValue].asJava).build())
  implicit val uuidEncoder: D4SAttributeEncoder[UUID]                = n => Some(AttributeValue.builder().s(n.toString).build())
  implicit val bytesEncoder: D4SAttributeEncoder[Array[Byte]]        = n => Some(AttributeValue.builder().b(SdkBytes.fromByteArray(n)).build())
  implicit val sdkBytesEncoder: D4SAttributeEncoder[SdkBytes]        = n => Some(AttributeValue.builder().b(n).build())

  implicit val binarySetSdkBytesEncoder: D4SAttributeEncoder[Set[SdkBytes]] =
    item => Some(AttributeValue.builder().bs(item.asJavaCollection).build())

  implicit val binarySetEncoder: D4SAttributeEncoder[Set[Array[Byte]]] =
    item => Some(AttributeValue.builder().bs(item.map(SdkBytes.fromByteArray).asJavaCollection).build())

  implicit val stringSetEncoder: D4SAttributeEncoder[Set[String]] =
    item => Some(AttributeValue.builder().ss(item.asJavaCollection).build())

  implicit val byteSetEncoder: D4SAttributeEncoder[Set[Byte]]     = item => Some(AttributeValue.builder().ns(item.map(_.toString).asJavaCollection).build())
  implicit val shortSetEncoder: D4SAttributeEncoder[Set[Short]]   = item => Some(AttributeValue.builder().ns(item.map(_.toString).asJavaCollection).build())
  implicit val intSetEncoder: D4SAttributeEncoder[Set[Int]]       = item => Some(AttributeValue.builder().ns(item.map(_.toString).asJavaCollection).build())
  implicit val longSetEncoder: D4SAttributeEncoder[Set[Long]]     = item => Some(AttributeValue.builder().ns(item.map(_.toString).asJavaCollection).build())
  implicit val floatSetEncoder: D4SAttributeEncoder[Set[Float]]   = item => Some(AttributeValue.builder().ns(item.map(_.toString).asJavaCollection).build())
  implicit val doubleSetEncoder: D4SAttributeEncoder[Set[Double]] = item => Some(AttributeValue.builder().ns(item.map(_.toString).asJavaCollection).build())

  implicit def stringTupleEncoder[S <: String, V: D4SAttributeEncoder]: D4SEncoder[(S, V)] = {
    case (k, v) => D4SAttributeEncoder.encodeField(k, v)
  }

  implicit def eitherEncoder[A: D4SAttributeEncoder, B: D4SAttributeEncoder]: D4SEncoder[Either[A, B]] = {
    D4SEncoder.derived
  }

  implicit def iterableEncoder[L[_], T: D4SAttributeEncoder](implicit ev: L[T] <:< Iterable[T]): D4SAttributeEncoder[L[T]] = {
    item: L[T] =>
      val ls = item.map(encode[T]).filter(_.nonEmpty).map(_.get)
      Some(AttributeValue.builder().l(ls.asJavaCollection).build())
  }

  implicit def mapLikeEncoder[M[k, v] <: scala.collection.Map[k, v], K: D4SKeyEncoder, V: D4SAttributeEncoder]: D4SEncoder[M[K, V]] = {
    _.map { case (k, v) => D4SKeyEncoder.encode(k) -> encode(v) }.toMap.filter(_._2.nonEmpty).view.mapValues(_.get).toMap
  }

  implicit def optionEncoder[T: D4SAttributeEncoder]: D4SAttributeEncoder[Option[T]] = {
    item: Option[T] => item.flatMap(encode[T])
  }

  private[this] def numericAttributeEncoder[NumericType]: D4SAttributeEncoder[NumericType] = n => Some(AttributeValue.builder().n(n.toString).build())

  private[this] final class CaseObjectEncoder[T](name: String) extends D4SAttributeEncoder[T] {
    override def encode(item: T): Option[AttributeValue] = Some(AttributeValue.builder().s(name).build())
  }
}
