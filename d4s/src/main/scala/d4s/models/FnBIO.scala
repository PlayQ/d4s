package d4s.models

import izumi.functional.bio.{BIO, F}

trait FnBIO[-A, +B] {
  self =>
  def apply[F[+_, +_]: BIO](a: A): F[Throwable, B]

  final def andThen[C](next: FnBIO[B, C]): FnBIO[A, C] = {
    new FnBIO[A, C] {
      override def apply[F[+_, +_]: BIO](a: A): F[Throwable, C] = {
        self.apply[F](a).flatMap(next.apply[F](_))
      }
    }
  }
}

object FnBIO {
  private[FnBIO] type UnknownF[+_, +_]

  @inline final def apply[A, B](f: A => BIO[UnknownF] => UnknownF[Throwable, B]): FnBIO[A, B] = new FnBIO[A, B] {
    override def apply[F[+_, +_]: BIO](a: A): F[Throwable, B] = {
      val castedF = f.asInstanceOf[A => BIO[F] => F[Throwable, B]]
      castedF(a)(F)
    }
  }

  @inline final def lift[A, B](f: A => B): FnBIO[A, B] = new FnBIO[A, B] {
    override def apply[F[+_, +_]: BIO](a: A): F[Throwable, B] = F.syncThrowable(f(a))
  }
}
