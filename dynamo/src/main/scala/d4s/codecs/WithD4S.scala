package d4s.codecs

import scala.reflect.macros.blackbox
import scala.language.experimental.macros

abstract class WithD4S[T](implicit val derivedCodec: DerivationDerivedCodec[T]) {
  def this(proxy: WithD4S[T]) = this()(DerivationDerivedCodec(proxy.enc, proxy.dec))

  implicit val enc: D4SEncoder[T] = derivedCodec.enc
  implicit val dec: D4SDecoder[T] = derivedCodec.dec
}

final class MaterializeDerivationMacros(val c: blackbox.Context) {
  import c.universe._

  def materializeCodec[A: c.WeakTypeTag]: c.Expr[DerivationDerivedCodec[A]] = c.Expr[DerivationDerivedCodec[A]] {
    q"""new ${weakTypeOf[DerivationDerivedCodec[A]]}(_root_.d4s.codecs.D4SEncoder.derive[${weakTypeOf[A]}], _root_.d4s.codecs.D4SDecoder.derive[${weakTypeOf[A]}])"""
  }
}

final case class DerivationDerivedCodec[A](enc: D4SEncoder[A], dec: D4SDecoder[A])
object DerivationDerivedCodecs {
  implicit def materialize[A]: DerivationDerivedCodec[A] = macro MaterializeDerivationMacros.materializeCodec[A]
}
