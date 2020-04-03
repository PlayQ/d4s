package d4s.models

import d4s.DynamoExecutionContext
import d4s.models.ExecutionStrategy.FThrowable
import d4s.models.query.{DynamoQuery, DynamoRequest}
import fs2.Stream

trait ExecutionStrategy[DR <: DynamoRequest, -Dec, +A] extends ExecutionStrategy.Dependent[DR, Dec, FThrowable[?[_, `+_`], A]] {
  def apply[F[+_, +_]](query: DynamoQuery[DR, Dec])(ctx: DynamoExecutionContext[F]): F[Throwable, A]
}

object ExecutionStrategy {

  private[models] type FThrowable[F[_, +_], +A]        = F[Throwable, A]
  private[models] type StreamFThrowable[+F[_, +_], +A] = Stream[F[Throwable, ?], A]

  private[models] type UnknownF[+_, +_]

  def apply[DR <: DynamoRequest, Dec, A](
    fn: DynamoQuery[DR, Dec] => DynamoExecutionContext[UnknownF] { type F[+a, +b] = UnknownF[a, b] } => UnknownF[Throwable, A]
  ): ExecutionStrategy[DR, Dec, A] =
    new ExecutionStrategy[DR, Dec, A] {
      override def apply[F[+_, +_]](query: DynamoQuery[DR, Dec])(ctx: DynamoExecutionContext[F]): F[Throwable, A] = {
        fn.asInstanceOf[DynamoQuery[DR, Dec] => DynamoExecutionContext[F] => F[Throwable, A]](query)(ctx)
      }
    }

  trait Streamed[DR <: DynamoRequest, -Dec, +A] extends ExecutionStrategy.Dependent[DR, Dec, StreamFThrowable[?[_, _], A]] {
    def apply[F[+_, +_]](query: DynamoQuery[DR, Dec])(ctx: DynamoExecutionContext[F]): Stream[F[Throwable, ?], A]
  }
  object Streamed {
    def apply[DR <: DynamoRequest, Dec, A](
      fn: DynamoQuery[DR, Dec] => DynamoExecutionContext[UnknownF] { type F[+a, +b] = UnknownF[a, b] } => StreamFThrowable[UnknownF, A]
    ): ExecutionStrategy.Streamed[DR, Dec, A] =
      new ExecutionStrategy.Streamed[DR, Dec, A] {
        override def apply[F[+_, +_]](query: DynamoQuery[DR, Dec])(ctx: DynamoExecutionContext[F]): StreamFThrowable[F, A] = {
          fn.asInstanceOf[DynamoQuery[DR, Dec] => DynamoExecutionContext[F] => StreamFThrowable[F, A]](query)(ctx)
        }
      }
  }

  trait Dependent[DR <: DynamoRequest, -Dec, +Out[_[_, _]]] {
    def apply[F[+_, +_]](query: DynamoQuery[DR, Dec])(ctx: DynamoExecutionContext[F]): Out[F]
  }

}
