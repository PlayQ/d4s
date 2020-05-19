package d4s

import izumi.reflect.Tag
import zio.Has

import scala.language.implicitConversions

object HasDynamoConnector {
  def apply[F[-_, +_, +_]](connector: DynamoConnector3[F])(implicit tag: Tag[DynamoConnector3[F]]): HasDynamoConnector[F] = Has(connector)

  implicit def fromConnector[F[-_, +_, +_]](connnector: DynamoConnector3[F])(implicit tag: Tag[DynamoConnector3[F]]): HasDynamoConnector[F] = Has(connnector)
}
