package d4s.models.table.index

import d4s.config.ProvisionedThroughputConfig
import d4s.models.table.DynamoKey
import software.amazon.awssdk.services.dynamodb.model.{GlobalSecondaryIndex, Projection}

final case class GlobalIndex[-H, -R](
  name: String,
  key: DynamoKey[H, R],
  projection: Projection,
) extends TableIndex[H, R] {
  def toAmz(provisionedThroughputConfig: ProvisionedThroughputConfig): GlobalSecondaryIndex =
    provisionedThroughputConfig
      .configureThroughput(
        GlobalSecondaryIndex
          .builder()
          .indexName(name)
          .keySchema(key.toJava)
          .projection(projection)
      )
      .build()
}
