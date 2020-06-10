package d4s.models.table

import d4s.config.{DynamoMeta, TableProvisionedThroughputConfig}
import d4s.models.table.index.{GlobalIndex, LocalIndex}

final case class TableDDL(
  private val tableReference: TableReference,
  globalIndexes: Set[GlobalIndex[_, _]],
  localIndexes: Set[LocalIndex[_, _]],
  additionalAttributes: Set[DynamoField[_]],
  provisioning: TableProvisionedThroughputConfig,
  backupEnabled: Option[Boolean],
) {
  def key: DynamoKey[_, _]     = tableReference.key
  def ttlField: Option[String] = tableReference.ttlField

  def withGlobalIndexes(ind: GlobalIndex[_, _]*): TableDDL =
    copy(globalIndexes = globalIndexes ++ ind.toSet)

  def withLocalIndexes(ind: LocalIndex[_, _]*): TableDDL =
    copy(localIndexes = localIndexes ++ ind.toSet)

  def withAdditionalAttributes(attr: DynamoField[_]*): TableDDL =
    copy(additionalAttributes = additionalAttributes ++ attr.toSet)

  def withUpdateContinuousBackups(isEnabled: Option[Boolean]): TableDDL =
    copy(backupEnabled = isEnabled)
}

object TableDDL {
  final val defaulTTLFieldName = "default_ttl_field"

  def apply(
    tableReference: TableReference,
    globalIndexes: Set[GlobalIndex[_, _]]     = Set.empty,
    localIndexes: Set[LocalIndex[_, _]]       = Set.empty,
    additionalAttributes: Set[DynamoField[_]] = Set.empty,
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
