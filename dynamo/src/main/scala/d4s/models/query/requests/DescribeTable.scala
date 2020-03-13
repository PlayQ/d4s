package d4s.models.query.requests

import d4s.models.query.DynamoRequest
import d4s.models.query.DynamoRequest.WithTableReference
import d4s.models.table.TableReference
import software.amazon.awssdk.services.dynamodb.model._

final case class DescribeTable(
  table: TableReference
) extends DynamoRequest
  with WithTableReference[DescribeTable] {

  override type Rq  = DescribeTableRequest
  override type Rsp = DescribeTableResponse

  override def withTableReference(t: TableReference => TableReference): DescribeTable = copy(table = t(table))

  def toAmz: DescribeTableRequest = {
    DescribeTableRequest
      .builder()
      .tableName(table.fullName)
      .build()
  }
}
