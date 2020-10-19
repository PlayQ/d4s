package d4s

import izumi.functional.bio.BlockingIO2
import software.amazon.awssdk.services.dynamodb.DynamoDbClient

trait DynamoClient[F[_, _]] {
  /** Lift a single blocking method of dynamo client */
  def raw[A](f: DynamoDbClient => A): F[Throwable, A]
}

object DynamoClient {
  def apply[F[_, _]: DynamoClient]: DynamoClient[F] = implicitly

  final class Impl[F[+_, +_]](
    blocking: BlockingIO2[F],
    dynamo: DynamoComponent,
  ) extends DynamoClient[F] {
    def raw[T](f: DynamoDbClient => T): F[Throwable, T] = {
      blocking.syncBlocking(f(dynamo.client))
    }
  }
}
