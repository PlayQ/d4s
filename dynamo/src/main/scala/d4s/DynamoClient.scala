package d4s

import izumi.functional.bio.BlockingIO
import shapeless.<:!<
import software.amazon.awssdk.services.dynamodb.DynamoDbClient

trait DynamoClient[F[_, _]] {
  /** Lift a single blocking method of dynamo client */
  def raw[A](f: DynamoDbClient => A)(implicit notInMonadSanityCheck: A <:!< F[_, _]): F[Throwable, A]
}

object DynamoClient {
  def apply[F[_, _]: DynamoClient]: DynamoClient[F] = implicitly

  final class Impl[F[+_, +_]](
    blocking: BlockingIO[F],
    dynamo: DynamoComponent
  ) extends DynamoClient[F] {

    def raw[T](f: DynamoDbClient => T)(implicit notInMonadSanityCheck: T <:!< F[_, _]): F[Throwable, T] = {
      blocking.syncBlocking(f(dynamo.client))
    }

  }
}
