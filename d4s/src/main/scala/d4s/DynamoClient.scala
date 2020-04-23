package d4s

import d4s.DynamoClient.NotMonad
import izumi.functional.bio.BlockingIO
import software.amazon.awssdk.services.dynamodb.DynamoDbClient

trait DynamoClient[F[_, _]] {
  /** Lift a single blocking method of dynamo client */
  def raw[A](f: DynamoDbClient => A)(implicit ev: NotMonad[A, F]): F[Throwable, A]
}

object DynamoClient {
  def apply[F[_, _]: DynamoClient]: DynamoClient[F] = implicitly

  final class Impl[F[+_, +_]](
    blocking: BlockingIO[F],
    dynamo: DynamoComponent
  ) extends DynamoClient[F] {

    def raw[T](f: DynamoDbClient => T)(implicit ev: NotMonad[T, F]): F[Throwable, T] = {
      blocking.syncBlocking(f(dynamo.client))
    }

  }

  @scala.annotation.implicitAmbiguous("${A} must not be a a monad of type ${F}")
  trait NotMonad[A, F[_, _]] extends Serializable
  implicit def empty[A, F[_, _]]: NotMonad[A, F]                     = new NotMonad[A, F] {}
  implicit def notInMonadAmbiguous1[A, F[_, _] >: A]: NotMonad[A, F] = empty
  implicit def notInMonadAmbiguous2[A, F[_, _] >: A]: NotMonad[A, F] = empty
}
