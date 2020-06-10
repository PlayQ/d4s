package d4s.models.table

import d4s.config.ProvisionedThroughputConfig
import d4s.models.table.index.{GlobalIndexUpdate, IndexToUpdate, ProvisionedGlobalIndex}
import software.amazon.awssdk.services.dynamodb.model.{BillingMode, TableDescription}

object TableDDLOps {
  final case class DDLdiff(indexToUpdate: IndexToUpdate, provisioning: Option[ProvisionedThroughputConfig])

  implicit final class DdlOps(ddl: TableDDL) {
    def ddlDiff(oldTableDescription: TableDescription): DDLdiff = {
      DDLdiff(indexDiff(ddl, oldTableDescription), provisioningThroughputDiff(ddl, oldTableDescription))
    }
  }

  private def indexDiff(newDDL: TableDDL, oldTableDescription: TableDescription) = {
    import scala.jdk.CollectionConverters._

    val oldSecondaryIndexes = oldTableDescription.globalSecondaryIndexes().asScala.map(i => i.indexName -> i).toMap
    val currentIndexNames   = newDDL.globalIndexes.map(_.name).toList
    val oldIndexNames       = oldSecondaryIndexes.keys.toList

    val namesToCreate = currentIndexNames diff oldIndexNames
    val namesToDelete = oldIndexNames diff currentIndexNames

    val indexesToUpd = currentIndexNames.filter {
      name =>
        oldSecondaryIndexes.get(name) match {
          case None => false
          case Some(value) =>
            val provisioning = newDDL.provisioning.getIndexProvisioning(name)
            provisioning.read != value.provisionedThroughput().readCapacityUnits() || provisioning.write != value.provisionedThroughput().writeCapacityUnits()
        }
    }

    val create: Set[ProvisionedGlobalIndex[_, _]] = newDDL.globalIndexes
      .filter(i => namesToCreate.contains(i.name))
      .map(i => ProvisionedGlobalIndex(i.name, i.key, i.projection, newDDL.provisioning.getIndexProvisioning(i.name)))

    val modify = newDDL.globalIndexes
      .filter(indexesToUpd.contains)
      .map(i => GlobalIndexUpdate(i.name, newDDL.provisioning.getIndexProvisioning(i.name)))

    val delete = oldTableDescription.globalSecondaryIndexes().asScala.filter(i => namesToDelete.contains(i.indexName())).map(_.indexName()).toSet

    IndexToUpdate(create, modify, delete)
  }

  private def provisioningThroughputDiff(newDDL: TableDDL, oldTableDescription: TableDescription): Option[ProvisionedThroughputConfig] = {
    val oldConfig = ProvisionedThroughputConfig(
      oldTableDescription.provisionedThroughput().readCapacityUnits(),
      oldTableDescription.provisionedThroughput().writeCapacityUnits(),
      Option(oldTableDescription.billingModeSummary()).flatMap(b => Option(b.billingMode())).getOrElse(BillingMode.UNKNOWN_TO_SDK_VERSION),
    )

    val currConfig = newDDL.provisioning.tableProvisioning
    if (currConfig.read != oldConfig.read || currConfig.write != oldConfig.write)
      Some(currConfig)
    else
      None
  }
}
