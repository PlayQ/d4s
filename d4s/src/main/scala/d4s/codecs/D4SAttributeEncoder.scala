package d4s.codecs

import java.util.UUID

import magnolia.{Magnolia, ReadOnlyCaseClass, SealedTrait}
import software.amazon.awssdk.core.SdkBytes
import software.amazon.awssdk.services.dynamodb.model.AttributeValue

import scala.language.experimental.macros

import scala.jdk.CollectionConverters._

trait D4SAttributeEncoder[T] {
  def encodeAttribute(item: T): AttributeValue

  def contramap[T1](f: T1 => T): D4SAttributeEncoder[T1]                        = item => encodeAttribute(f(item))
  def mapAttribute(f: AttributeValue => AttributeValue): D4SAttributeEncoder[T] = item => f(encodeAttribute(item))
}

object D4SAttributeEncoder {
  def apply[T: D4SAttributeEncoder]: D4SAttributeEncoder[T] = implicitly

  def encodeAttribute[T: D4SAttributeEncoder](item: T): AttributeValue                        = D4SAttributeEncoder[T].encodeAttribute(item)
  def encodePlain[T: D4SAttributeEncoder](name: String, item: T): Map[String, AttributeValue] = Map(name -> D4SAttributeEncoder[T].encodeAttribute(item))

  /** Magnolia instances */
  def derived[T]: D4SAttributeEncoder[T] = macro Magnolia.gen[T]
  type Typeclass[T] = D4SAttributeEncoder[T]
  def combine[T](ctx: ReadOnlyCaseClass[D4SAttributeEncoder, T]): D4SAttributeEncoder[T] = {
    item =>
      if (ctx.isObject) {
        AttributeValue.builder().s(ctx.typeName.short).build()
      } else {
        val map = ctx.parameters.map {
          p =>
            p.label -> p.typeclass.encodeAttribute(p.dereference(item))
        }.toMap
        AttributeValue.builder().m(map.asJava).build()
      }
  }

  def dispatch[T](ctx: SealedTrait[D4SAttributeEncoder, T]): D4SAttributeEncoder[T] = {
    item =>
      ctx.dispatch(item) {
        subtype =>
          subtype.typeclass.encodeAttribute(subtype.cast(item))
      }
  }

  implicit val attributeEncoder: D4SAttributeEncoder[AttributeValue] = a => a
  implicit val stringEncoder: D4SAttributeEncoder[String]            = AttributeValue.builder().s(_).build()
  implicit val boolEncoder: D4SAttributeEncoder[Boolean]             = AttributeValue.builder().bool(_).build()
  implicit val byteEncoder: D4SAttributeEncoder[Byte]                = numericAttributeEncoder[Byte]
  implicit val shortEncoder: D4SAttributeEncoder[Short]              = numericAttributeEncoder[Short]
  implicit val intEncoder: D4SAttributeEncoder[Int]                  = numericAttributeEncoder[Int]
  implicit val longEncoder: D4SAttributeEncoder[Long]                = numericAttributeEncoder[Long]
  implicit val floatEncoder: D4SAttributeEncoder[Float]              = numericAttributeEncoder[Float]
  implicit val doubleEncoder: D4SAttributeEncoder[Double]            = numericAttributeEncoder[Double]
  implicit val unitEncoder: D4SAttributeEncoder[Unit]                = _ => AttributeValue.builder().m(Map.empty[String, AttributeValue].asJava).build()
  implicit val uuidEncoder: D4SAttributeEncoder[UUID]                = n => AttributeValue.builder().s(n.toString).build()

  implicit val bytesEncoder: D4SAttributeEncoder[Array[Byte]] = n => AttributeValue.builder().b(SdkBytes.fromByteArray(n)).build()
  implicit val sdkBytesEncoder: D4SAttributeEncoder[SdkBytes] = n => AttributeValue.builder().b(n).build()

  implicit def binarySetEncoder: D4SAttributeEncoder[Set[Array[Byte]]] =
    item => AttributeValue.builder().bs(item.map(SdkBytes.fromByteArray).asJavaCollection).build()

  implicit def iterableEncoder[L[_], T](implicit T: D4SAttributeEncoder[T], ev0: L[T] <:< Iterable[T]): D4SAttributeEncoder[L[T]] = {
    item: L[T] =>
      val ls = item.map(T.encodeAttribute)
      AttributeValue.builder().l(ls.asJavaCollection).build()
  }

  implicit def mapLikeEncoder[M[k, v] <: scala.collection.Map[k, v], K, V](implicit V: D4SAttributeEncoder[V], K: D4SKeyEncoder[K]): D4SEncoder[M[K, V]] = {
    _.map { case (k, v) => K.encode(k) -> V.encodeAttribute(v) }.toMap
  }

  implicit def optionEncoder[T](implicit T: D4SAttributeEncoder[T]): D4SAttributeEncoder[Option[T]] = {
    item: Option[T] =>
      item.map(T.encodeAttribute).getOrElse(AttributeValue.builder().nul(true).build())
  }

  implicit def eitherEncoder[A: D4SAttributeEncoder, B: D4SAttributeEncoder]: D4SAttributeEncoder[Either[A, B]] = D4SEncoder.nonCastedGen[Either[A, B]]

  private[this] def numericAttributeEncoder[NumericType]: D4SAttributeEncoder[NumericType] = n => AttributeValue.builder().n(n.toString).build()
}
