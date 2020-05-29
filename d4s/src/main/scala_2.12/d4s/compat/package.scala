package d4s

package object compat {

  private[d4s] object chaining {

    implicit final class ChainingOps[A](val a: A) extends AnyVal {
      def tap[U](f: A => U): A = {
        f(a)
        a
      }

      def pipe[B](f: A => B): B = f(a)
    }
  }
}
