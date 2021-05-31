package d4s.models.table

import d4s.config.{DynamoMeta, TableProvisionedThroughputConfig}
import d4s.models.table.index.{GlobalIndex, LocalIndex}

final case class TableDDL(
  private val tableReference: TableReference,
  globalIndexes: Set[GlobalIndex[?, ?]],
  localIndexes: Set[LocalIndex[?, ?]],
  additionalAttributes: Set[DynamoField[?]],
  provisioning: TableProvisionedThroughputConfig,
  backupEnabled: Option[Boolean],
) {
  def key: DynamoKey[?, ?]     = tableReference.key
  def ttlField: Option[String] = tableReference.ttlField

  def withGlobalIndexes(ind: GlobalIndex[?, ?]*): TableDDL =
    copy(globalIndexes = globalIndexes ++ ind.toSet)

  def withLocalIndexes(ind: LocalIndex[?, ?]*): TableDDL =
    copy(localIndexes = localIndexes ++ ind.toSet)

  def withAdditionalAttributes(attr: DynamoField[?]*): TableDDL =
    copy(additionalAttributes = additionalAttributes ++ attr.toSet)

  def withUpdateContinuousBackups(isEnabled: Option[Boolean]): TableDDL =
    copy(backupEnabled = isEnabled)
}

object TableDDL {
  final val defaulTTLFieldName = "default_ttl_field"

  def apply(
    tableReference: TableReference,
    globalIndexes: Set[GlobalIndex[?, ?]]     = Set.empty,
    localIndexes: Set[LocalIndex[?, ?]]       = Set.empty,
    additionalAttributes: Set[DynamoField[?]] = Set.empty,
  )(implicit dynamoMeta: DynamoMeta
  ): TableDDL = {
    new TableDDL(
      tableReference       = tableReference,
      globalIndexes        = globalIndexes,
      localIndexes         = localIndexes,
      additionalAttributes = additionalAttributes,
      provisioning         = dynamoMeta.getProvisioning(tableReference.tableName),
      dynamoMeta.backupEnabled,
    )
  }
}
