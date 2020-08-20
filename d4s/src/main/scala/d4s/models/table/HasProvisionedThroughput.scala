package d4s.models.table

import software.amazon.awssdk.services.dynamodb.model._

trait HasProvisionedThroughput[A] {
  def provisionedThroughput(a: A, provisionedThroughput: ProvisionedThroughput): A
}

object HasProvisionedThroughput {
  @inline def apply[A: HasProvisionedThroughput]: HasProvisionedThroughput[A] = implicitly

  implicit val hasProvisionedThroughputCreateTableRequest: HasProvisionedThroughput[CreateTableRequest.Builder]                             = _.provisionedThroughput(_)
  implicit val hasProvisionedThroughputUpdateTableRequest: HasProvisionedThroughput[UpdateTableRequest.Builder]                             = _.provisionedThroughput(_)
  implicit val hasProvisionedThroughputGlobalSecondaryIndex: HasProvisionedThroughput[GlobalSecondaryIndex.Builder]                         = _.provisionedThroughput(_)
  implicit val hasProvisionedThroughputUpdateGlobalSecondaryIndexAction: HasProvisionedThroughput[UpdateGlobalSecondaryIndexAction.Builder] = _.provisionedThroughput(_)
  implicit val hasProvisionedThroughputCreateGlobalSecondaryIndexAction: HasProvisionedThroughput[CreateGlobalSecondaryIndexAction.Builder] = _.provisionedThroughput(_)
}
