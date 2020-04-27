package d4s.codecs

import d4s.codecs.CodecsUtils.CastedMagnolia
import magnolia.{Magnolia, ReadOnlyCaseClass, SealedTrait}
import software.amazon.awssdk.services.dynamodb.model.AttributeValue

import scala.jdk.CollectionConverters._
import scala.language.experimental.macros

trait D4SEncoder[A] extends D4SAttributeEncoder[A] {
  def encode(item: A): Map[String, AttributeValue]
  def encodeJava(item: A): java.util.Map[String, AttributeValue] = encode(item).asJava
  override final def encodeAttribute(item: A): AttributeValue    = AttributeValue.builder().m(encodeJava(item)).build()

  override final def contramap[B](f: B => A): D4SEncoder[B]                                         = item => encode(f(item))
  final def mapObject(f: Map[String, AttributeValue] => Map[String, AttributeValue]): D4SEncoder[A] = item => f(encode(item))
  final def contramap2[B, C](another: D4SEncoder[B])(f: C => (A, B)): D4SEncoder[C] = {
    item =>
      val (a, b) = f(item)
      encode(a) ++ another.encode(b)
  }
  def appendFields[Item: D4SEncoder](f: (A, Map[String, AttributeValue]) => Item): D4SEncoder[A] = {
    item =>
      val encoded = encode(item)
      encoded ++ D4SEncoder.encode(f(item, encoded))
  }
}

object D4SEncoder {
  @inline def apply[T: D4SEncoder]: D4SEncoder[T] = implicitly
  def derived[T]: D4SEncoder[T] = macro CastedMagnolia.genWithCast[T, D4SEncoder[T]]

  private[d4s] def nonCastedGen[T]: D4SAttributeEncoder[T] = macro Magnolia.gen[T]

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
