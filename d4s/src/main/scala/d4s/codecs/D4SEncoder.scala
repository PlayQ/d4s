package d4s.codecs

import magnolia._
import software.amazon.awssdk.services.dynamodb.model.AttributeValue

import scala.jdk.CollectionConverters._
import scala.language.experimental.macros

trait D4SEncoder[T] extends D4SAttributeEncoder[T] {
  self =>
  def encode(item: T): Map[String, AttributeValue]
  def encodeJava(item: T): java.util.Map[String, AttributeValue] = encode(item).asJava
  override final def encodeAttribute(item: T): AttributeValue    = AttributeValue.builder().m(encodeJava(item)).build()

  override final def contramap[T1](f: T1 => T): D4SEncoder[T1]                                      = item => encode(f(item))
  final def mapObject(f: Map[String, AttributeValue] => Map[String, AttributeValue]): D4SEncoder[T] = item => f(encode(item))
  final def contramap2[T1, A](another: D4SEncoder[T1])(f: A => (T, T1)): D4SEncoder[A] = {
    item =>
      val (t, t1) = f(item)
      encode(t) ++ another.encode(t1)
  }
}

object D4SEncoder {
  def apply[T: D4SEncoder]: D4SEncoder[T] = implicitly
  def derived[T]: D4SEncoder[T] = macro Magnolia.gen[T]

  def encode[T: D4SEncoder](item: T): Map[String, AttributeValue]               = D4SEncoder[T].encode(item)
  def encodeJava[T: D4SEncoder](item: T): java.util.Map[String, AttributeValue] = D4SEncoder[T].encodeJava(item)
  def encodeAttribute[T: D4SAttributeEncoder](item: T): AttributeValue          = D4SAttributeEncoder[T].encodeAttribute(item)

  /** Magnolia instances. */
  type Typeclass[T] = D4SAttributeEncoder[T]
  def combine[T](ctx: ReadOnlyCaseClass[D4SAttributeEncoder, T]): D4SEncoder[T] = {
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
}
