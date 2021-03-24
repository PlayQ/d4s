package d4s.models

import cats.arrow.FunctionK
import cats.~>
import d4s.DynamoInterpreter
import d4s.models.ExecutionStrategy.{FThrowable, StrategyInput}
import d4s.models.query.{DynamoQuery, DynamoRequest}
import fs2.Stream
import izumi.functional.bio.{Async2, Temporal2}

import scala.reflect.ClassTag

trait ExecutionStrategy[DR <: DynamoRequest, -Dec, +A] extends ExecutionStrategy.Dependent[DR, Dec, FThrowable[?[`+_`, `+_`], A]] {
  def apply[F[+_, +_]](input: StrategyInput[F, DR, Dec]): F[Throwable, A]
}

object ExecutionStrategy {
  final case class StrategyInput[F[+_, +_], DR <: DynamoRequest, +Dec](
    query: DynamoQuery[DR, Dec],
    implicit val F: Async2[F],
    implicit val FT: Temporal2[F],
    implicit val interpreter: DynamoInterpreter[F],
    streamExecutionWrapper: F[Throwable, ?] ~> F[Throwable, ?],
    interpreterErrorLogger: PartialFunction[DynamoException, F[Nothing, Unit]],
  ) {
    def tapInterpreterError(f: PartialFunction[DynamoException, F[Nothing, Unit]]): StrategyInput[F, DR, Dec] = {
      copy(interpreterErrorLogger = f orElse this.interpreterErrorLogger)
    }
    def discardInterpreterError[Err: ClassTag]: StrategyInput[F, DR, Dec] = {
      tapInterpreterError { case DynamoException(_, _: Err) => F.unit }
    }
  }
  object StrategyInput {
    def apply[F[+_, +_]: Async2: Temporal2, DR <: DynamoRequest, Dec](
      query: DynamoQuery[DR, Dec],
      interpreter: DynamoInterpreter[F],
      streamExecutionWrapper: F[Throwable, ?] ~> F[Throwable, ?]                 = FunctionK.id[F[Throwable, ?]],
      interpreterErrorLogger: PartialFunction[DynamoException, F[Nothing, Unit]] = PartialFunction.empty,
    ): StrategyInput[F, DR, Dec] = {
      new StrategyInput(query, implicitly, implicitly, interpreter, streamExecutionWrapper, interpreterErrorLogger)
    }
  }

  private[models] type FThrowable[F[+_, +_], +A]        = F[Throwable, A]
  private[models] type StreamFThrowable[+F[+_, +_], +A] = Stream[F[Throwable, ?], A]

  private[models] type UnknownF[+_, +_]

  def apply[DR <: DynamoRequest, Dec, A](
    fn: StrategyInput[UnknownF, DR, Dec] { type F[+a, +b] = UnknownF[a, b] } => UnknownF[Throwable, A]
  ): ExecutionStrategy[DR, Dec, A] =
    new ExecutionStrategy[DR, Dec, A] {
      override def apply[F[+_, +_]](input: StrategyInput[F, DR, Dec]): F[Throwable, A] = {
        fn.asInstanceOf[StrategyInput[F, DR, Dec] => F[Throwable, A]](input)
      }
    }

  trait Streamed[DR <: DynamoRequest, -Dec, +A] extends ExecutionStrategy.Dependent[DR, Dec, StreamFThrowable[?[`+_`, `+_`], A]] {
    def apply[F[+_, +_]](input: StrategyInput[F, DR, Dec]): Stream[F[Throwable, ?], A]
  }
  object Streamed {
    def apply[DR <: DynamoRequest, Dec, A](
      fn: StrategyInput[UnknownF, DR, Dec] { type F[+a, +b] = UnknownF[a, b] } => StreamFThrowable[UnknownF, A]
    ): ExecutionStrategy.Streamed[DR, Dec, A] =
      new ExecutionStrategy.Streamed[DR, Dec, A] {
        override def apply[F[+_, +_]](input: StrategyInput[F, DR, Dec]): StreamFThrowable[F, A] = {
          fn.asInstanceOf[StrategyInput[F, DR, Dec] => StreamFThrowable[F, A]](input)
        }
      }
  }

  trait Dependent[DR <: DynamoRequest, -Dec, +Out[_[+_, +_]]] {
    def apply[F[+_, +_]](input: StrategyInput[F, DR, Dec]): Out[F]
  }

}
