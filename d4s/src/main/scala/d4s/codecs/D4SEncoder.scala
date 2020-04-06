package d4s.codecs

import d4s.codecs.CodecsUtils.CastedMagnolia
import magnolia._
import software.amazon.awssdk.services.dynamodb.model.AttributeValue

import scala.jdk.CollectionConverters._
import scala.language.experimental.macros

trait D4SEncoder[T] extends D4SAttributeEncoder[T] {
  self =>
  def encode(item: T): Map[String, AttributeValue]

  def encodeJava(item: T): java.util.Map[String, AttributeValue] = {
    encode(item).asJava
  }

  def encodeAttribute(item: T): AttributeValue = {
    AttributeValue.builder().m(encodeJava(item)).build()
  }

  def contramap[T1](f: T1 => T): D4SEncoder[T1] = item => encode(f(item))
  def contramap2[T1, A](another: D4SEncoder[T1])(f: A => (T, T1)): D4SEncoder[A] = {
    item =>
      val (t, t1) = f(item)
      self.encode(t) ++ another.encode(t1)
  }
}

object D4SEncoder {
  def apply[T: D4SEncoder]: D4SEncoder[T] = implicitly
  def derived[T]: D4SEncoder[T] = macro CastedMagnolia.genWithCast[T, D4SEncoder[_]]

  def encode[T: D4SEncoder](item: T): Map[String, AttributeValue]               = D4SEncoder[T].encode(item)
  def encodeJava[T: D4SEncoder](item: T): java.util.Map[String, AttributeValue] = D4SEncoder[T].encodeJava(item)
  def encodeAttribute[T: D4SAttributeEncoder](item: T): AttributeValue          = D4SAttributeEncoder[T].encodeAttribute(item)

  /** Magnolia instances. */
  type Typeclass[T] = D4SAttributeEncoder[T]
  def combine[T](ctx: CaseClass[D4SAttributeEncoder, T]): D4SEncoder[T] = {
    item =>
      ctx.parameters.map {
        p =>
          p.label -> p.typeclass.encodeAttribute(p.dereference(item))
      }.toMap
  }
  def dispatch[T](ctx: SealedTrait[D4SAttributeEncoder, T]): D4SEncoder[T] = {
    item =>
      ctx.dispatch(item) {
        subtype =>
          Map(subtype.typeName.short -> subtype.typeclass.encodeAttribute(subtype.cast(item)))
      }
  }

  // special case, we don't need to do anything with Map[String, AttributeValue]
  implicit val attributeMapEncoder: D4SEncoder[Map[String, AttributeValue]] = a => a

  implicit def plainMapEncoder[T: D4SAttributeEncoder]: D4SEncoder[Map[String, T]] =
    m => m.mapValues(D4SAttributeEncoder[T].encodeAttribute(_)).toMap
}
