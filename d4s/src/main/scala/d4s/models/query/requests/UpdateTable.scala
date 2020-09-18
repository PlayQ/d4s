package d4s.models.query.requests

import d4s.config.ProvisionedThroughputConfig
import d4s.models.query.DynamoRequest
import d4s.models.query.DynamoRequest.WithTableReference
import d4s.models.table.TableReference
import d4s.models.table.index.{GlobalIndexUpdate, ProvisionedGlobalIndex}
import software.amazon.awssdk.services.dynamodb.model._

import scala.jdk.CollectionConverters._
import scala.util.chaining._

final case class UpdateTable(
  table: TableReference,
  newProvisioning: Option[ProvisionedThroughputConfig] = None,
  newGlobalIndex: Option[ProvisionedGlobalIndex[_, _]] = None,
  updateGlobalIndexes: Set[GlobalIndexUpdate]          = Set.empty,
  deleteGlobalIndex: Option[String]                    = None,
) extends DynamoRequest
  with WithTableReference[UpdateTable] {

  override type Rq  = UpdateTableRequest
  override type Rsp = UpdateTableResponse

  override def withTableReference(t: TableReference => TableReference): UpdateTable = copy(table = t(table))

  def withNewProvisioning(provisioning: ProvisionedThroughputConfig): UpdateTable = copy(newProvisioning = Some(provisioning))

  def withIndexToCreate(index: ProvisionedGlobalIndex[_, _]): UpdateTable = copy(newGlobalIndex = Some(index))

  def withIndexesToUpdate(indexes: Set[GlobalIndexUpdate]): UpdateTable = copy(updateGlobalIndexes = indexes)

  def withIndexToDelete(index: String): UpdateTable = copy(deleteGlobalIndex = Some(index))

  override def toAmz: UpdateTableRequest = {
    val allAttributes: Seq[AttributeDefinition] = {
      Seq(
        table.key.toAttributes,
        newGlobalIndex.map(_.key.toAttributes).getOrElse(List.empty),
      ).flatten.distinct
    }

    val newIndexes = newGlobalIndex.map(_.asCreateAction).map {
      GlobalSecondaryIndexUpdate.builder().create(_).build()
    }

    val indexToUpdate = updateGlobalIndexes.map(_.asUpdateAction).map {
      GlobalSecondaryIndexUpdate.builder().update(_).build()
    }

    val indexToRemove = deleteGlobalIndex
      .map(DeleteGlobalSecondaryIndexAction.builder().indexName(_).build())
      .map(GlobalSecondaryIndexUpdate.builder().delete(_).build())

    val allUpdates = newIndexes ++ indexToUpdate ++ indexToRemove

    UpdateTableRequest
      .builder()
      .tableName(table.fullName)
      .attributeDefinitions(allAttributes.asJavaCollection)
      .pipe(b => if (allUpdates.isEmpty) b else b.globalSecondaryIndexUpdates(allUpdates.toList.asJavaCollection))
      .pipe(builder => newProvisioning.fold(builder)(_.configure(builder)))
      .build()
  }
}
