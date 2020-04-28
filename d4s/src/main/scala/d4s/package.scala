import zio.Has

package object d4s {
  type DynamoConnector3[F[-_, +_, +_]]   = DynamoConnector[F[Any, +?, +?]]
  type HasDynamoConnector[F[-_, +_, +_]] = Has[DynamoConnector[F[Any, +?, +?]]]
}
