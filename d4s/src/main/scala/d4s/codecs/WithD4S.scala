package d4s.codecs

import scala.language.experimental.macros
import scala.reflect.macros.blackbox

//abstract class WithD4S[T]()(implicit val derivedCodec: D4SDerivedCodec[T]) {
//  implicit val codec: D4SCodec[T] = D4SCodec.fromPair(derivedCodec.enc, derivedCodec.dec)
//}

final class MaterializeDerivationMacros(val c: blackbox.Context) {
  import c.universe._

  def materializeCodec[A: c.WeakTypeTag]: c.Expr[D4SDerivedCodec[A]] = c.Expr[D4SDerivedCodec[A]] {
    q"""new ${weakTypeOf[D4SDerivedCodec[A]]}(_root_.d4s.codecs.D4SEncoder.derived[${weakTypeOf[A]}], _root_.d4s.codecs.D4SDecoder.derived[${weakTypeOf[A]}])"""
  }
}

final case class D4SDerivedCodec[A](enc: D4SEncoder[A], dec: D4SDecoder[A])
object D4SDerivedCodec {
  implicit def materialize[A]: D4SDerivedCodec[A] = macro MaterializeDerivationMacros.materializeCodec[A]
}
