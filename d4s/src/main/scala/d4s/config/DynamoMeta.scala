package d4s.config

import net.playq.aws.tagging.AwsNameSpace

final case class DynamoMeta(
  provisioningConfig: ProvisioningConfig,
  nameSpace: AwsNameSpace,
  backupEnabled: Option[Boolean],
) {
  def getProvisioning(tableName: String): TableProvisionedThroughputConfig = {
    provisioningConfig.tables
      .find(_.tableName.contains(tableName))
      .getOrElse(TableProvisionedThroughputConfig(tableName, provisioningConfig.default, perIndex = Nil))
  }
}
