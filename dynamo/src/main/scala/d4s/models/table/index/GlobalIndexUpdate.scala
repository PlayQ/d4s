package d4s.models.table.index

import d4s.config.ProvisionedThroughputConfig
import software.amazon.awssdk.services.dynamodb.model.UpdateGlobalSecondaryIndexAction

final case class GlobalIndexUpdate(indexName: String, newProvisioning: ProvisionedThroughputConfig) {

  def asUpdateAction: UpdateGlobalSecondaryIndexAction = {
    newProvisioning.configureThroughput {
      UpdateGlobalSecondaryIndexAction
        .builder()
        .indexName(indexName)
    }.build()
  }
}
