package d4s.models.table

import d4s.config.{DynamoMeta, TableProvisionedThroughputConfig}
import d4s.models.table.index.{GlobalIndex, LocalIndex}

final case class TableDDL(
  private val tableReference: TableReference,
  globalIndexes: Set[GlobalIndex[Nothing, Nothing]],
  localIndexes: Set[LocalIndex[Nothing, Nothing]],
  additionalAttributes: Set[DynamoField[?]],
  provisioning: TableProvisionedThroughputConfig,
  backupEnabled: Option[Boolean],
) {
  def key: DynamoKey[Nothing, Nothing] = tableReference.key
  def ttlField: Option[String]         = tableReference.ttlField

  def withGlobalIndexes(ind: GlobalIndex[Nothing, Nothing]*): TableDDL =
    copy(globalIndexes = globalIndexes ++ ind.toSet)

  def withLocalIndexes(ind: LocalIndex[Nothing, Nothing]*): TableDDL =
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
    globalIndexes: Set[GlobalIndex[Nothing, Nothing]] = Set.empty,
    localIndexes: Set[LocalIndex[Nothing, Nothing]]   = Set.empty,
    additionalAttributes: Set[DynamoField[?]]         = Set.empty,
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
