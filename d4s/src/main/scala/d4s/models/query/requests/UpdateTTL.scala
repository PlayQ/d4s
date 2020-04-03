package d4s.models.query.requests

import d4s.models.table.TableReference
import d4s.models.query.DynamoRequest
import d4s.models.query.DynamoRequest.WithTableReference
import software.amazon.awssdk.services.dynamodb.model.{TimeToLiveSpecification, UpdateTimeToLiveRequest, UpdateTimeToLiveResponse}

final case class UpdateTTL(
  table: TableReference
) extends DynamoRequest
  with WithTableReference[UpdateTTL] {

  override type Rq  = UpdateTimeToLiveRequest
  override type Rsp = UpdateTimeToLiveResponse

  override def withTableReference(t: TableReference => TableReference): UpdateTTL = copy(table = t(table))

  override def toAmz: UpdateTimeToLiveRequest = {
    table.ttlField.fold(throw new RuntimeException("Table reference does not contain ttlField. Must be a bug.")) {
      ttlField =>
        val ttlSpec = TimeToLiveSpecification.builder().enabled(true).attributeName(ttlField).build()
        UpdateTimeToLiveRequest.builder().tableName(table.fullName).timeToLiveSpecification(ttlSpec).build()
    }
  }
}
