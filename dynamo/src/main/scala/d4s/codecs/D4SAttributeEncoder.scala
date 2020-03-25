package d4s.codecs

import software.amazon.awssdk.core.SdkBytes
import software.amazon.awssdk.services.dynamodb.model.AttributeValue

import scala.jdk.CollectionConverters._

trait D4SAttributeEncoder[T] {
  def encodeAttribute(item: T): AttributeValue
}

object D4SAttributeEncoder {
  def apply[T: D4SAttributeEncoder]: D4SAttributeEncoder[T] = implicitly

  final def encodeAttribute[T: D4SAttributeEncoder](item: T): AttributeValue = D4SAttributeEncoder[T].encodeAttribute(item)
  final def encodePlain[T: D4SAttributeEncoder](name: String, item: T): Map[String, AttributeValue] =
    Map(name -> D4SAttributeEncoder[T].encodeAttribute(item))

  implicit val attributeEncoder: D4SAttributeEncoder[AttributeValue] = a => a
  implicit val stringEncoder: D4SAttributeEncoder[String]            = AttributeValue.builder().s(_).build()
  implicit val boolEncoder: D4SAttributeEncoder[Boolean]             = AttributeValue.builder().bool(_).build()
  implicit val intEncoder: D4SAttributeEncoder[Int]                  = n => AttributeValue.builder().n(n.toString).build()
  implicit val longEncoder: D4SAttributeEncoder[Long]                = n => AttributeValue.builder().n(n.toString).build()
  implicit val doubleEncoder: D4SAttributeEncoder[Double]            = n => AttributeValue.builder().n(n.toString).build()
  implicit val unitEncoder: D4SAttributeEncoder[Unit]                = _ => AttributeValue.builder().m(Map.empty[String, AttributeValue].asJava).build()

  implicit val bytesEncoder: D4SAttributeEncoder[Array[Byte]] = n => AttributeValue.builder().b(SdkBytes.fromByteArray(n)).build()
  implicit def binarySetEncoder: D4SAttributeEncoder[Set[Array[Byte]]] =
    item => AttributeValue.builder().bs(item.map(SdkBytes.fromByteArray).asJavaCollection).build()

  implicit def iterableEncoder[L[_], T](implicit T: D4SAttributeEncoder[T], ev0: L[T] <:< Iterable[T]): D4SAttributeEncoder[L[T]] = {
    item: L[T] =>
      val ls = item.map(T.encodeAttribute)
      AttributeValue.builder().l(ls.asJavaCollection).build()
  }

  implicit def mapLikeEncoder[M[x] <: scala.collection.Map[String, x], V](implicit V: D4SAttributeEncoder[V]): D4SAttributeEncoder[M[V]] = {
    item: M[V] =>
      val map = item.map { case (str, attr) => str -> V.encodeAttribute(attr) }.asJava
      AttributeValue.builder().m(map).build()
  }

  implicit def optionEncoder[T](implicit T: D4SAttributeEncoder[T]): D4SAttributeEncoder[Option[T]] = {
    item: Option[T] =>
      item.map(T.encodeAttribute).getOrElse(AttributeValue.builder().nul(true).build())
  }
}
