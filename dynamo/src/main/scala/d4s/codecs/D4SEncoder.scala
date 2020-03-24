package d4s.codecs

import d4s.codecs.CodecsUtils.CastedMagnolia
import magnolia._
import software.amazon.awssdk.core.SdkBytes
import software.amazon.awssdk.services.dynamodb.model.AttributeValue

import scala.jdk.CollectionConverters._
import scala.language.experimental.macros

trait D4SAttributeEncoder[T] {
  def encodeAttribute(item: T): AttributeValue
}
object D4SAttributeEncoder {
  def apply[T: D4SAttributeEncoder]: D4SAttributeEncoder[T] = implicitly

  final def encodeAttribute[T: D4SAttributeEncoder](item: T): AttributeValue = D4SAttributeEncoder[T].encodeAttribute(item)
  final def encodePlain[T: D4SAttributeEncoder](name: String, item: T): Map[String, AttributeValue] =
    Map(name -> D4SAttributeEncoder[T].encodeAttribute(item))

  implicit val stringEncoder: D4SAttributeEncoder[String] = AttributeValue.builder().s(_).build()
  implicit val boolEncoder: D4SAttributeEncoder[Boolean]  = AttributeValue.builder().bool(_).build()
  implicit val intEncoder: D4SAttributeEncoder[Int]       = n => AttributeValue.builder().n(n.toString).build()
  implicit val longEncoder: D4SAttributeEncoder[Long]     = n => AttributeValue.builder().n(n.toString).build()
  implicit val doubleEncoder: D4SAttributeEncoder[Double] = n => AttributeValue.builder().n(n.toString).build()
  implicit val unitEncoder: D4SAttributeEncoder[Unit]     = _ => AttributeValue.builder().m(Map.empty[String, AttributeValue].asJava).build()

  implicit val bytesEncoder: D4SAttributeEncoder[Array[Byte]] = n => AttributeValue.builder().b(SdkBytes.fromByteArray(n)).build()
  implicit def binarySetEncoder: D4SAttributeEncoder[Set[Array[Byte]]] =
    item => AttributeValue.builder().bs(item.map(SdkBytes.fromByteArray).asJavaCollection).build()

  implicit def iterableEncoder[L[_], T](implicit T: D4SAttributeEncoder[T], ev0: L[T] <:< Iterable[T]): D4SAttributeEncoder[L[T]] = {
    item: L[T] =>
      val ls = item.map(T.encodeAttribute)
      AttributeValue.builder().l(ls.asJavaCollection).build()
  }

  implicit def mapLikeEncoder[K, V, M[K, V] <: Map[K, V]](implicit V: D4SAttributeEncoder[V]): D4SAttributeEncoder[M[K, V]] = {
    item: M[K, V] =>
      val map = item.map[String, AttributeValue] { case (str, attr) => str.toString -> V.encodeAttribute(attr) }.asJava
      AttributeValue.builder().m(map).build()
  }

  implicit def optionEncoder[T](implicit T: D4SAttributeEncoder[T]): D4SAttributeEncoder[Option[T]] = {
    item: Option[T] =>
      item.map(T.encodeAttribute).getOrElse(AttributeValue.builder().nul(true).build())
  }
}

trait D4SEncoder[T] extends D4SAttributeEncoder[T] {
  def encode(item: T): Map[String, AttributeValue]

  def encodeJava(item: T): java.util.Map[String, AttributeValue] = {
    encode(item).asJava
  }

  override final def encodeAttribute(item: T): AttributeValue = {
    AttributeValue.builder().m(encodeJava(item)).build()
  }
}

object D4SEncoder {
  def apply[T: D4SEncoder]: D4SEncoder[T] = implicitly

  def encode[T: D4SEncoder](item: T): Map[String, AttributeValue]               = D4SEncoder[T].encode(item)
  def encodeJava[T: D4SEncoder](item: T): java.util.Map[String, AttributeValue] = D4SEncoder[T].encodeJava(item)
  final def encodeAttribute[T: D4SAttributeEncoder](item: T): AttributeValue    = D4SAttributeEncoder[T].encodeAttribute(item)

  type Typeclass[T] = D4SAttributeEncoder[T]

  def derived[T]: D4SEncoder[T] = macro CastedMagnolia.genWithCast[T, D4SEncoder[Nothing]]

  def combine[T](ctx: CaseClass[D4SAttributeEncoder, T]): D4SEncoder[T] = new D4SEncoder[T] {
    override def encode(item: T): Map[String, AttributeValue] = {
      ctx.parameters.map {
        p =>
          p.label -> p.typeclass.encodeAttribute(p.dereference(item))
      }.toMap
    }
  }

  def dispatch[T](ctx: SealedTrait[D4SAttributeEncoder, T]): D4SEncoder[T] = new D4SEncoder[T] {
    override def encode(item: T): Map[String, AttributeValue] = {
      ctx.dispatch(item) {
        subtype =>
          Map(subtype.typeName.short -> subtype.typeclass.encodeAttribute(subtype.cast(item)))
      }
    }
  }
}
