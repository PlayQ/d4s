package d4s.codecs.`macro`

import d4s.codecs.D4SAttributeEncoder

import scala.reflect.macros.blackbox

object MacroUtils {
  def attributeEncoderDefault[T: c.WeakTypeTag](c: blackbox.Context): c.Expr[D4SAttributeEncoder[T]] = {
    import c.universe._
    c.Expr[D4SAttributeEncoder[T]](q"""_root_.d4s.codecs.D4SAttributeEncoder.Default.derived[${weakTypeOf[T]}]""")
  }
  def attributeEncoderDropNulls[T: c.WeakTypeTag](c: blackbox.Context): c.Expr[D4SAttributeEncoder[T]] = {
    import c.universe._
    c.Expr[D4SAttributeEncoder[T]](q"""_root_.d4s.codecs.D4SAttributeEncoder.WithoutNulls.derived[${weakTypeOf[T]}]""")
  }
}
