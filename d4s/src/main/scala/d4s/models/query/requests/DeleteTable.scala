package d4s.models.query.requests

import d4s.models.table.TableReference
import d4s.models.query.DynamoRequest
import d4s.models.query.DynamoRequest.WithTableReference
import software.amazon.awssdk.services.dynamodb.model.{DeleteTableRequest, DeleteTableResponse}

final case class DeleteTable(
  table: TableReference
) extends DynamoRequest
  with WithTableReference[DeleteTable] {

  override type Rq  = DeleteTableRequest
  override type Rsp = DeleteTableResponse

  override def withTableReference(t: TableReference => TableReference): DeleteTable = copy(table = t(table))

  def toAmz: DeleteTableRequest = {
    DeleteTableRequest
      .builder()
      .tableName(table.fullName)
      .build()
  }
}
