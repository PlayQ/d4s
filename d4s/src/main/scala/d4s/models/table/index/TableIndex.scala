package d4s.models.table.index

import d4s.models.table.DynamoKey
import software.amazon.awssdk.services.dynamodb.model.Projection

trait TableIndex[-H, -R] {
  val name: String
  val key: DynamoKey[H, R]
  val projection: Projection
}
