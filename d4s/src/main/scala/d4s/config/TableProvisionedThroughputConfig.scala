package d4s.config

final case class IndexProvisionedThroughputConfig(
  indexName: String,
  provisioning: ProvisionedThroughputConfig,
)

/**
  * @param tableName regex matching the tables this configuration should apply to
  */
final case class TableProvisionedThroughputConfig(
  tableName: String,
  tableProvisioning: ProvisionedThroughputConfig,
  perIndex: List[IndexProvisionedThroughputConfig],
) {
  def getIndexProvisioning(index: String): ProvisionedThroughputConfig = {
    perIndex.find(_.indexName == index).map(_.provisioning).getOrElse(tableProvisioning)
  }
}
