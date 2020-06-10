package d4s.models.table.index

import d4s.models.table.DynamoKey
import software.amazon.awssdk.services.dynamodb.model.{LocalSecondaryIndex, Projection}

final case class LocalIndex[-H, -R](
  name: String,
  key: DynamoKey[H, R],
  projection: Projection,
) extends TableIndex[H, R] {
  def toAmz: LocalSecondaryIndex =
    LocalSecondaryIndex
      .builder()
      .indexName(name)
      .keySchema(key.toJava)
      .projection(projection)
      .build()
}
