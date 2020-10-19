package d4s.models

import izumi.functional.bio.{IO2, F}

trait FnIO2[-A, +B] {
  self =>
  def apply[F[+_, +_]: IO2](a: A): F[Throwable, B]

  final def andThen[C](next: FnIO2[B, C]): FnIO2[A, C] = {
    new FnIO2[A, C] {
      override def apply[F[+_, +_]: IO2](a: A): F[Throwable, C] = {
        self.apply[F](a).flatMap(next.apply[F](_))
      }
    }
  }
}

object FnIO2 {
  private[FnIO2] type UnknownF[+_, +_]

  @inline final def apply[A, B](f: A => IO2[UnknownF] => UnknownF[Throwable, B]): FnIO2[A, B] = new FnIO2[A, B] {
    override def apply[F[+_, +_]: IO2](a: A): F[Throwable, B] = {
      val castedF = f.asInstanceOf[A => IO2[F] => F[Throwable, B]]
      castedF(a)(F)
    }
  }

  @inline final def lift[A, B](f: A => B): FnIO2[A, B] = new FnIO2[A, B] {
    override def apply[F[+_, +_]: IO2](a: A): F[Throwable, B] = F.syncThrowable(f(a))
  }
}
