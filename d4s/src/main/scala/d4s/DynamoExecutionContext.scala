package d4s

import cats.arrow.FunctionK
import cats.~>
import izumi.functional.bio.BIOTemporal

trait DynamoExecutionContext[F[+_, +_]] {
  implicit val F: BIOTemporal[F]
  implicit val interpreter: DynamoInterpreter[F]
  private[d4s] val streamExecutionWrapper: F[Throwable, ?] ~> F[Throwable, ?]
}

object DynamoExecutionContext {
  def apply[F[+_, +_]](F0: BIOTemporal[F],
                       interpreter0: DynamoInterpreter[F],
                       streamExecutionWrapper0: F[Throwable, ?] ~> F[Throwable, ?] = FunctionK.id[F[Throwable, ?]]): DynamoExecutionContext[F] = {
    new DynamoExecutionContext[F] {
      override val F: BIOTemporal[F]                                          = F0
      override val interpreter: DynamoInterpreter[F]                          = interpreter0
      override val streamExecutionWrapper: F[Throwable, ?] ~> F[Throwable, ?] = streamExecutionWrapper0
    }
  }
}
