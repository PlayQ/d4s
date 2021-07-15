package d4s.codecs

import d4s.codecs.D4SEncoder.traitEncoder
import magnolia.{Magnolia, ReadOnlyCaseClass, SealedTrait}
import software.amazon.awssdk.services.dynamodb.model.AttributeValue

import scala.jdk.CollectionConverters._
import scala.language.experimental.macros

trait D4SEncoder[A] extends D4SAttributeEncoder[A] {
  override final def encode(item: A): AttributeValue = AttributeValue.builder().m(encodeObjectJava(item)).build()
  def encodeObject(item: A): Map[String, AttributeValue]
  final def encodeObjectJava(item: A): java.util.Map[String, AttributeValue] = encodeObject(item).asJava

  override final def contramap[B](f: B => A): D4SEncoder[B] = item => encodeObject(f(item))
  final def contramap2[B, C](another: D4SEncoder[B])(f: C => (A, B)): D4SEncoder[C] = {
    item =>
      val (a, b) = f(item)
      encodeObject(a) ++ another.encodeObject(b)
  }
  final def postprocessObjectEncoder(f: Map[String, AttributeValue] => Map[String, AttributeValue]): D4SEncoder[A] = item => f(encodeObject(item))
  def appendFields[Item: D4SEncoder](f: (A, Map[String, AttributeValue]) => Item): D4SEncoder[A] = {
    item =>
      val encoded = encodeObject(item)
      encoded ++ D4SEncoder.encodeObject(f(item, encoded))
  }
}

abstract class GenericD4SEncoder(dropNulls: Boolean) {
  def combineImpl[T](ctx: ReadOnlyCaseClass[D4SAttributeEncoder, T]): D4SEncoder[T]  = {
    item => {
      val result = ctx.parameters.map {
        p =>
          p.label -> p.typeclass.encode(p.dereference(item))
      }.toMap
      if (dropNulls) result.view.filter { case (_, v) => !v.nul() }.toMap else result
    }
  }
  def derived[A]: D4SEncoder[A] = macro Magnolia.gen[A]

  /** Magnolia instances. */
  private[GenericD4SEncoder] type Typeclass[T] = D4SAttributeEncoder[T]

  def combine[T](ctx: ReadOnlyCaseClass[D4SAttributeEncoder, T]): D4SEncoder[T] = combineImpl(ctx)

  def dispatch[T](ctx: SealedTrait[D4SAttributeEncoder, T]): D4SEncoder[T] = {
    traitEncoder[T](ctx.dispatch(_)(subtype => subtype.typeName.short -> subtype.typeclass))
  }
}

object D4SEncoder extends GenericD4SEncoder(false) {
  @inline def apply[A](implicit ev: D4SEncoder[A]): ev.type = ev

  def encode[A: D4SAttributeEncoder](item: A): AttributeValue                                = D4SAttributeEncoder[A].encode(item)
  def encodeObject[A: D4SEncoder](item: A): Map[String, AttributeValue]                      = D4SEncoder[A].encodeObject(item)
  def encodeObjectJava[A: D4SEncoder](item: A): java.util.Map[String, AttributeValue]        = D4SEncoder[A].encodeObjectJava(item)
  def encodeField[A: D4SAttributeEncoder](key: String, item: A): Map[String, AttributeValue] = D4SAttributeEncoder.encodeField(key, item)

  def traitEncoder[A](caseMap: A => (String, D4SAttributeEncoder[? <: A])): D4SEncoder[A] = {
    item =>
      val typeNameEncoder = caseMap(item)
      Map(typeNameEncoder._1 -> typeNameEncoder._2.asInstanceOf[D4SAttributeEncoder[A]].encode(item))
  }

  object WithoutNulls extends GenericD4SEncoder(true)
}
