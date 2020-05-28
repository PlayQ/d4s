package d4s.models.table.index

import d4s.config.{ProvisionedThroughputConfig, TableProvisionedThroughputConfig}
import d4s.models.table.DynamoKey
import software.amazon.awssdk.services.dynamodb.model.{CreateGlobalSecondaryIndexAction, Projection}

final case class ProvisionedGlobalIndex[-H, -R](
  name: String,
  key: DynamoKey[H, R],
  projection: Projection,
  provisionedThroughputConfig: ProvisionedThroughputConfig,
) extends TableIndex[H, R] {

  def asCreateAction: CreateGlobalSecondaryIndexAction = {
    provisionedThroughputConfig.configureThroughput {
      CreateGlobalSecondaryIndexAction
        .builder()
        .indexName(name)
        .keySchema(key.toJava)
        .projection(projection)
    }.build()
  }
}

object ProvisionedGlobalIndex {
  implicit final class FromGlobalIndex[H, R](private val index: GlobalIndex[H, R]) extends AnyVal {
    def toProvisionedIndex(cfg: TableProvisionedThroughputConfig): ProvisionedGlobalIndex[H, R] = {
      ProvisionedGlobalIndex(index.name, index.key, index.projection, cfg.getIndexProvisioning(index.name))
    }
  }
}
