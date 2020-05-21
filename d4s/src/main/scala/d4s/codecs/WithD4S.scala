package d4s.codecs

import d4s.codecs.WithD4S.D4SDerivedCodec

import scala.language.experimental.macros
import scala.reflect.macros.blackbox

abstract class WithD4S[T]()(implicit derivedCodec: D4SDerivedCodec[T]) {
  implicit val codec: D4SCodec[T] = D4SCodec.fromPair(derivedCodec.enc, derivedCodec.dec)
}

object WithD4S {
  object MaterializeDerivationMacros {

    def materializeCodec[A: c.WeakTypeTag](c: blackbox.Context): c.Expr[D4SDerivedCodec[A]] = c.Expr[D4SDerivedCodec[A]] {
      import c.universe._
      q"new ${weakTypeOf[D4SDerivedCodec[A]]}(_root_.d4s.codecs.D4SEncoder.derived[${weakTypeOf[A]}], _root_.d4s.codecs.D4SDecoder.derived[${weakTypeOf[A]}])"
    }

    def materializeAttributeCodec[A: c.WeakTypeTag](c: blackbox.Context): c.Expr[D4SDerivedAttributeCodec[A]] = c.Expr[D4SDerivedAttributeCodec[A]] {
      import c.universe._
      q"new ${weakTypeOf[D4SDerivedAttributeCodec[A]]}(_root_.d4s.codecs.D4SAttributeEncoder.derived[${weakTypeOf[A]}], _root_.d4s.codecs.D4SDecoder.derived[${weakTypeOf[A]}])"
    }

  }

  final case class D4SDerivedCodec[A](enc: D4SEncoder[A], dec: D4SDecoder[A])
  object D4SDerivedCodec {
    implicit def materialize[A]: D4SDerivedCodec[A] = macro MaterializeDerivationMacros.materializeCodec[A]
  }

  final case class D4SDerivedAttributeCodec[A](enc: D4SAttributeEncoder[A], dec: D4SDecoder[A])
  object D4SDerivedAttributeCodec {
    implicit def materialize[A]: D4SDerivedAttributeCodec[A] = macro MaterializeDerivationMacros.materializeAttributeCodec[A]
  }
}
