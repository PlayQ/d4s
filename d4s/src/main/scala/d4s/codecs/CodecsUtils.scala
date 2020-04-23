package d4s.codecs

import magnolia.Magnolia

import scala.reflect.macros.whitebox

object CodecsUtils {
  object CastedMagnolia {
    def genWithCast[T: c.WeakTypeTag, R: c.WeakTypeTag](c: whitebox.Context): c.Tree = {
      import c.universe._
      val resultType = appliedType(weakTypeOf[R], weakTypeOf[T])
      q"${Magnolia.gen[T](c)}.asInstanceOf[$resultType]"
    }
  }
}
