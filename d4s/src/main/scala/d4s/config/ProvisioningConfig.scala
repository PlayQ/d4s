package d4s.config

final case class ProvisioningConfig(
  default: ProvisionedThroughputConfig,
  tables: List[TableProvisionedThroughputConfig],
)
