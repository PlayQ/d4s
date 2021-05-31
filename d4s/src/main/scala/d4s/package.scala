import zio.Has

package object d4s {
  type DynamoConnector3[F[-_, +_, +_]]   = DynamoConnector[F[Any, + _, + _]]
  type HasDynamoConnector[F[-_, +_, +_]] = Has[DynamoConnector[F[Any, + _, + _]]]
}
